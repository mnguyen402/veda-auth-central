environment = "dev"
namespace   = "veda-auth-central"
region      = "us-east-2"

vpc_cidr     = "10.20.30.0/24"
public_cidr  = "10.20.30.0/25"
private_cidr = "10.20.30.128/25"


keycloak_alb_certificate_arn = "arn:aws:acm:us-east-2:444055461661:certificate/f1d83468-c073-49f9-bbfa-b038135ae400"
keycloak_dns_name            = "auth.veda.usecustos.org"
dns_zone_id                  = "Z0928161AO8DT4S0II3Z"

container_cpu_units                = 1024
container_memory_limit             = 2048
container_memory_reserved          = 1024
jvm_heap_min                       = 512
jvm_heap_max                       = 1024
jvm_meta_min                       = 128
jvm_meta_max                       = 512
deployment_maximum_percent         = 100
deployment_minimum_healthy_percent = 50
desired_count                      = 1
log_retention_days                 = 5

db_instance_type         = "db.r6g.large"
db_backup_retention_days = 5
db_cluster_size          = 2


vault_alb_certificate_arn   = "arn:aws:acm:us-east-2:444055461661:certificate/bad63635-9ba1-4f74-9342-5c5d24ea94a9"
vault_ami                   = "ami-0b8b44ec9a8f90422"
ec2_ssh_key_name            = "veda-auth"
vault_instance_type         = "m5.xlarge"
vault_version               = "1.11.0"
vault_leader_tls_servername = "vault.veda.usecustos.org"
vault_secrets_manager_arn   = "arn:aws:secretsmanager:us-east-2:444055461661:secret:vault-tls-certificates-tAwR0u"
vault_min_nodes             = 1
vault_max_nodes             = 5
