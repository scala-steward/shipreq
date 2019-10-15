# aws ssm get-parameters --region us-east-2 --names /aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2
# aws ec2 describe-images --region us-east-2 --owners amazon --filters 'Name=name,Values=amzn2-ami-hvm-2*-x86_64-gp2' 'Name=state,Values=available' --query 'reverse(sort_by(Images, &CreationDate))[:1]'
data "aws_ssm_parameter" "ami-ec2" {
  name = "/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2"
}

data "aws_ssm_parameter" "ami-ecs" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2/recommended/image_id"
}
