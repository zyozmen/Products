# cleanup-aws.ps1
$ErrorActionPreference = "Continue"

$AWS_REGION   = "us-east-2"
$ECR_REPO     = "products-service"
$CLUSTER_NAME = "products-cluster"
$SERVICE_NAME = "products-service"
$LOG_GROUP    = "/ecs/products-service"
$ROLE_NAME    = "products-ecs-task-execution-role"
$SG_NAME      = "products-app-sg"
$S3_BUCKET    = "terraform-state-505231787824"
$DYNAMO_TABLE = "terraform-locks"

Write-Host "=== 1. Destruyendo infraestructura con Terraform ===" -ForegroundColor Yellow
if (Test-Path "./terraform.exe") {
    ./terraform.exe destroy -auto-approve -var="image_tag=cleanup"
} elseif (Get-Command terraform -ErrorAction SilentlyContinue) {
    terraform destroy -auto-approve -var="image_tag=cleanup"
}

Write-Host "=== 2. Eliminando Servicio y Cluster de ECS ===" -ForegroundColor Yellow
aws ecs update-service --cluster $CLUSTER_NAME --service $SERVICE_NAME --desired-count 0 --region $AWS_REGION
aws ecs delete-service --cluster $CLUSTER_NAME --service $SERVICE_NAME --force --region $AWS_REGION
aws ecs delete-cluster --cluster $CLUSTER_NAME --region $AWS_REGION

Write-Host "=== 3. Eliminando Repositorio ECR ===" -ForegroundColor Yellow
aws ecr delete-repository --repository-name $ECR_REPO --force --region $AWS_REGION

Write-Host "=== 4. Eliminando Security Group ===" -ForegroundColor Yellow
$SG_ID = (aws ec2 describe-security-groups --group-names $SG_NAME --region $AWS_REGION --query "SecurityGroups[0].GroupId" --output text 2>$null)
if ($SG_ID -and $SG_ID -ne "None") {
    aws ec2 delete-security-group --group-id $SG_ID --region $AWS_REGION
}

Write-Host "=== 5. Eliminando CloudWatch Log Group ===" -ForegroundColor Yellow
aws logs delete-log-group --log-group-name $LOG_GROUP --region $AWS_REGION

Write-Host "=== 6. Eliminando Rol de IAM ===" -ForegroundColor Yellow
aws iam detach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
aws iam delete-role --role-name $ROLE_NAME

Write-Host "=== 7. Eliminando Tabla DynamoDB ===" -ForegroundColor Yellow
aws dynamodb delete-table --table-name $DYNAMO_TABLE --region $AWS_REGION

Write-Host "=== 8. Vaciando y Eliminando Bucket S3 ===" -ForegroundColor Yellow
aws s3 rm "s3://$S3_BUCKET" --recursive
aws s3api delete-objects --bucket $S3_BUCKET --delete "$(aws s3api list-object-versions --bucket $S3_BUCKET --query '{Objects: Versions[].{Key:Key,VersionId:VersionId}}' --output json)" 2>$null
aws s3api delete-objects --bucket $S3_BUCKET --delete "$(aws s3api list-object-versions --bucket $S3_BUCKET --query '{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' --output json)" 2>$null
aws s3 rb "s3://$S3_BUCKET" --force

Write-Host "✅ Limpieza completada exitosamente. Cuenta en $0." -ForegroundColor Green