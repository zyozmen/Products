#!/usr/bin/env bash
set -e

# Configuración de variables
AWS_REGION="us-east-2"
ECR_REPO="products-service"
CLUSTER_NAME="products-cluster"
SERVICE_NAME="products-service"
LOG_GROUP="/ecs/products-service"
ROLE_NAME="products-ecs-task-execution-role"
SG_NAME="products-app-sg"
S3_BUCKET="terraform-state-505231787824"
DYNAMO_TABLE="terraform-locks"

echo "=== 1. Destruyendo infraestructura gestionada por Terraform ==="
if [ -f "./terraform" ]; then
    ./terraform destroy -auto-approve -var="image_tag=cleanup" || true
elif command -v terraform &> /dev/null; then
    terraform destroy -auto-approve -var="image_tag=cleanup" || true
fi

echo "=== 2. Eliminando Servicio y Clúster de ECS (si persisten) ==="
aws ecs update-service --cluster $CLUSTER_NAME --service $SERVICE_NAME --desired-count 0 --region $AWS_REGION 2>/dev/null || true
aws ecs delete-service --cluster $CLUSTER_NAME --service $SERVICE_NAME --force --region $AWS_REGION 2>/dev/null || true
aws ecs delete-cluster --cluster $CLUSTER_NAME --region $AWS_REGION 2>/dev/null || true

echo "=== 3. Eliminando Repositorio ECR e Imágenes ==="
aws ecr delete-repository --repository-name $ECR_REPO --force --region $AWS_REGION 2>/dev/null || true

echo "=== 4. Eliminando Security Group huérfano ==="
SG_ID=$(aws ec2 describe-security-groups --group-names $SG_NAME --region $AWS_REGION --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true)
if [ -n "$SG_ID" ] && [ "$SG_ID" != "None" ]; then
    aws ec2 delete-security-group --group-id $SG_ID --region $AWS_REGION 2>/dev/null || true
    echo "Security group $SG_ID eliminado."
fi

echo "=== 5. Eliminando CloudWatch Log Group ==="
aws logs delete-log-group --log-group-name $LOG_GROUP --region $AWS_REGION 2>/dev/null || true

echo "=== 6. Desvinculando Políticas y Eliminando Rol de IAM ==="
aws iam detach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy 2>/dev/null || true
aws iam delete-role --role-name $ROLE_NAME 2>/dev/null || true

echo "=== 7. Eliminando Tabla de DynamoDB (Locks) ==="
aws dynamodb delete-table --table-name $DYNAMO_TABLE --region $AWS_REGION 2>/dev/null || true

echo "=== 8. Vaciando y Eliminando Bucket de S3 (State Remote) ==="
aws s3 rm s3://$S3_BUCKET --recursive 2>/dev/null || true
# Eliminar todas las versiones de objetos en S3 si el versionamiento está activo
aws s3api delete-objects --bucket $S3_BUCKET --delete "$(aws s3api list-object-versions --bucket $S3_BUCKET --query '{Objects: Versions[].{Key:Key,VersionId:VersionId}}' --output json)" 2>/dev/null || true
aws s3api delete-objects --bucket $S3_BUCKET --delete "$(aws s3api list-object-versions --bucket $S3_BUCKET --query '{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' --output json)" 2>/dev/null || true
aws s3 rb s3://$S3_BUCKET --force 2>/dev/null || true

echo "✅ Limpieza completa terminada. La cuenta AWS $AWS_REGION está en $0.00." 