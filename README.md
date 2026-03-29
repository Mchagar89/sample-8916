# sample-8916

## Overview

This repository contains a Spring Boot REST service (`/api/hello`) and a GitHub Actions pipeline to build a Docker image, push it to Amazon ECR, then deploy to Amazon EKS.

## Local run

1. Install Java 17 and Maven.
2. Run `./run.sh`.
3. Browse `http://localhost:8080/api/hello`.

## AWS GitHub Actions setup

Set the following secrets in the repository:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` (e.g. `us-west-2`)
- `AWS_ACCOUNT_ID` (your AWS account number)
- `EKS_CLUSTER_NAME` (your EKS cluster name)

The workflow is defined in `.github/workflows/aws-eks-deploy.yml`.

## Kubernetes manifests

- `k8s/deployment.yaml` (deployment for `demo-app`)
- `k8s/service.yaml` (LoadBalancer service)

## API

- GET `/api/hello` returns `Hello from Spring Boot shell app!`
