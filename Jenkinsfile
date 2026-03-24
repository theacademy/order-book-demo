pipeline {
    agent any

    environment {
        // Map branch to environment (master=prod, uat=uat, develop=dev)
        ENV = "${env.BRANCH_NAME == 'master' ? 'prod' : (env.BRANCH_NAME == 'uat' ? 'uat' : 'dev')}"

        // Application configuration
        APP_NAME = 'orderbook'
        APP_PORT = '8085'
        MIN_COVERAGE = '70'

        // Environment-specific namespace
        NAMESPACE = "${ENV}"

        // Environment-specific configurations
        REPLICAS = "${ENV == 'prod' ? '5' : (ENV == 'uat' ? '3' : '2')}"

        // AWS Configuration - Use credentials per environment
        AWS_REGION = credentials("aws-region-${ENV}")
        EKS_CLUSTER_NAME = credentials("eks-cluster-${ENV}")
        AWS_ACCOUNT_ID = credentials("aws-account-id-${ENV}")
        ECR_REPOSITORY = credentials("ecr-repository-${ENV}")
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

        // Git branch validation
        SAFE_BRANCH = "${env.BRANCH_NAME?.replaceAll('[^a-zA-Z0-9-]', '-') ?: 'unknown'}"
    }

    tools {
        maven 'MAVEN_HOME'
        jdk 'JAVA_HOME'
    }

    stages {
        stage('Environment Validation') {
            steps {
                script {
                    echo """
                    ============================================
                    DEPLOYMENT CONFIGURATION
                    ============================================
                    Environment: ${ENV}
                    Branch: ${env.BRANCH_NAME}
                    Namespace: ${NAMESPACE}
                    Cluster: ${EKS_CLUSTER_NAME}
                    Replicas: ${REPLICAS}
                    ============================================
                    """

                    if (ENV == 'prod' && env.BRANCH_NAME != 'master') {
                        error "Production deployments only allowed from 'master' branch"
                    }
                    if (ENV == 'uat' && env.BRANCH_NAME != 'uat') {
                        error "UAT deployments only allowed from 'uat' branch"
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                git(
                    url: 'https://github.com/theacademy/order-book-demo.git',
                    branch: env.BRANCH_NAME,
                    credentialsId: 'github-credentials'
                )
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "Git commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        stage('Build & Test') {
            when {
                expression { ENV != 'prod' }
            }
            parallel {
                stage('Compile') {
                    steps {
                        sh 'mvn compile'
                    }
                }
                stage('Code Review') {
                    steps {
                        sh 'mvn pmd:pmd'
                    }
                    post {
                        success {
                            recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
                        }
                    }
                }
                stage('Test with Coverage') {
                    steps {
                        sh 'mvn clean test jacoco:report -Pcoverage'
                    }
                }
            }
        }

        stage('Publish Test Reports') {
            when {
                expression { ENV != 'prod' }
            }
            steps {
                junit 'target/surefire-reports/*.xml'
                recordCoverage(
                    tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                    sourceCodeRetention: 'MODIFIED'
                )
                archiveArtifacts artifacts: 'target/site/jacoco/**', fingerprint: true

                script {
                    def coverage = sh(
                        script: "grep -oP 'ctr2=\"\\K[0-9]+' target/site/jacoco/jacoco.xml | head -1",
                        returnStdout: true
                    ).trim()

                    if (coverage && coverage.toInteger() < MIN_COVERAGE.toInteger()) {
                        error "Coverage ${coverage}% below threshold ${MIN_COVERAGE}%"
                    }
                    echo "Coverage: ${coverage}% passed"
                }
            }
        }

        stage('Package') {
            when {
                expression { ENV != 'prod' }
            }
            steps {
                sh 'mvn package -DskipTests'
                sh 'sha256sum target/orderbook.war > target/orderbook.war.sha256'
                archiveArtifacts artifacts: 'target/*.war, target/*.sha256'
            }
        }

        stage('Docker Build & Push') {
            steps {
                withAWS(credentials: "aws-credentials-${ENV}", region: "${AWS_REGION}") {
                    script {
                        def imageTag = "${ENV}-${BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                        def fullImageName = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${imageTag}"

                        sh """
                            aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                            docker build --build-arg JAR_FILE=target/orderbook.war \\
                                        --label "environment=${ENV}" \\
                                        --label "build.id=${BUILD_NUMBER}" \\
                                        --label "build.commit=${env.GIT_COMMIT_SHORT}" \\
                                        -t ${fullImageName} .
                            docker push ${fullImageName}
                        """
                    }
                }
            }
        }

        stage('Deploy to EKS') {
            steps {
                withAWS(credentials: "aws-credentials-${ENV}", region: "${AWS_REGION}") {
                    script {
                        def imageTag = "${ENV}-${BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                        def fullImageName = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${imageTag}"

                        sh """
                            aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER_NAME}
                            kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                            cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${APP_NAME}
  namespace: ${NAMESPACE}
spec:
  replicas: ${REPLICAS}
  selector:
    matchLabels:
      app: ${APP_NAME}
  template:
    metadata:
      labels:
        app: ${APP_NAME}
        environment: ${ENV}
    spec:
      containers:
      - name: ${APP_NAME}
        image: ${fullImageName}
        ports:
        - containerPort: ${APP_PORT}
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "${ENV}"
---
apiVersion: v1
kind: Service
metadata:
  name: ${APP_NAME}
  namespace: ${NAMESPACE}
spec:
  selector:
    app: ${APP_NAME}
  type: ${ENV == 'prod' ? 'LoadBalancer' : 'ClusterIP'}
  ports:
  - port: 80
    targetPort: ${APP_PORT}
EOF

                            kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=5m
                        """
                    }
                }
            }
        }

        stage('Manual Approval') {
            when {
                expression { ENV == 'prod' }
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
            }
        }
    }

    post {
        success {
            echo "Deployment to ${ENV} successful!"
        }
        failure {
            echo "Deployment to ${ENV} failed!"
        }
        always {
            cleanWs()
        }
    }
}