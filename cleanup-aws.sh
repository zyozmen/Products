#!/usr/bin/env bash
set -e

REGION="us-east-2"
S3_BUCKET="terraform-state-505231787824"
STATE_KEY="products-service/terraform.tfstate"
DYNAMO_TABLE="terraform-locks"
POLICY_ARN="arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"

echo "=== 1. Deteniendo y eliminando Servicios de ECS ==="
aws ecs update-service --region $REGION --cluster products-cluster --service products-api-service --desired-count 0 2>/dev/null || true
aws ecs delete-service --region $REGION --cluster products-cluster --service products-api-service --force 2>/dev/null || true

aws ecs update-service --region $REGION --cluster products-cluster --service products-service --desired-count 0 2>/dev/null || true
aws ecs delete-service --region $REGION --cluster products-cluster --service products-service --force 2>/dev/null || true

echo "=== 2. Eliminando Clúster ECS ==="
aws ecs delete-cluster --region $REGION --cluster products-cluster 2>/dev/null || true

echo "=== 3. Eliminando CloudWatch Log Group ==="
aws logs delete-log-group --region $REGION --log-group-name "/ecs/products-service" 2>/dev/null || true

echo "=== 4. Eliminando Security Groups ==="
for SG_NAME in "products-api-ecs-sg" "products-app-sg"; do
    SG_ID=$(aws ec2 describe-security-groups --region $REGION --filters "Name=group-name,Values=$SG_NAME" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true)
    if [ -n "$SG_ID" ] && [ "$SG_ID" != "None" ]; then
        # Elimina reglas dependientes antes de borrar el SG
        aws ec2 describe-security-group-rules --region $REGION --filters "Name=group-id,Values=$SG_ID" --query "SecurityGroupRules[*].SecurityGroupRuleId" --output text | xargs -n 1 aws ec2 revoke-security-group-ingress --region $REGION --group-id "$SG_ID" --security-group-rule-ids 2>/dev/null || true
        aws ec2 delete-security-group --region $REGION --group-id "$SG_ID" 2>/dev/null || true
    fi
done

echo "=== 5. Eliminando Roles IAM ==="
for ROLE in "products-api-ecs-execution-role" "products-ecs-task-execution-role"; do
    aws iam detach-role-policy --role-name $ROLE --policy-arn $POLICY_ARN 2>/dev/null || true
    aws iam delete-role --role-name $ROLE 2>/dev/null || true
done

echo "=== 6. Eliminando Repositorio ECR ==="
aws ecr delete-repository --region $REGION --repository-name "products-service" --force 2>/dev/null || true

echo "=== 7. Destruyendo Estado Remoto Corrupto (S3 y DynamoDB) ==="
aws s3 rm "s3://${S3_BUCKET}/${STATE_KEY}" --region $REGION 2>/dev/null || true
aws dynamodb delete-item --table-name $DYNAMO_TABLE --key "{\"LockID\": {\"S\": \"${S3_BUCKET}/${STATE_KEY}-md5\"}}" --region $REGION 2>/dev/null || true
aws dynamodb delete-item --table-name $DYNAMO_TABLE --key "{\"LockID\": {\"S\": \"${S3_BUCKET}/${STATE_KEY}\"}}" --region $REGION 2>/dev/null || true

echo "=== 8. Limpiando Security Group de Mongo ==="
SG_ID=$(aws ec2 describe-security-groups --region us-east-2 --filters "Name=group-name,Values=products-api-ecs-sg" --query "SecurityGroups[0].GroupId" --output text)

echo "Security Group detectado: $SG_ID"

# 2. Revocar reglas de Ingress/Egress asociadas
aws ec2 describe-security-group-rules --region us-east-2 --filters "Name=group-id,Values=$SG_ID" --query "SecurityGroupRules[*].SecurityGroupRuleId" --output text | xargs -n 1 aws ec2 revoke-security-group-ingress --region us-east-2 --group-id "$SG_ID" --security-group-rule-ids 2>/dev/null || true

# 3. Eliminar la regla de ingress creada en la EC2 de Mongo que apunta a este SG
aws ec2 revoke-security-group-ingress --region us-east-2 --group-id $(aws ec2 describe-security-groups --region us-east-2 --filters "Name=ip-permission.group-id,Values=$SG_ID" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || true) --protocol tcp --port 27017 --source-group "$SG_ID" 2>/dev/null || true

# 4. Eliminar el Security Group definitivamente
aws ec2 delete-security-group --region us-east-2 --group-id "$SG_ID"



echo "=== Limpieza terminada con éxito ==="