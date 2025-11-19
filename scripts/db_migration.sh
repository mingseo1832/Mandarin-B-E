#!/bin/bash

# 배포 경로 및 JAR 파일 이름 설정 (appspec.yml의 destination과 일치해야 함)
DEPLOY_PATH="/home/ec2-user/app"
# 가장 최근에 복사된 JAR 파일을 찾아 사용합니다.
JAR_NAME=$(ls -tr $DEPLOY_PATH/*.jar | tail -n 1)

echo "> DB 마이그레이션 실행: $JAR_NAME"

# 웹 서버를 시작하지 않고 (NONE), 포트를 사용하지 않으며 (port=0), 
# 프로덕션 환경 프로필로 DB 마이그레이션 후 즉시 종료되도록 (exit(0)) 실행합니다.
java -jar $JAR_NAME --spring.profiles.active=prod \
                    --spring.main.web-application-type=NONE \
                    --server.port=0 \
                    exit(0)

# 마이그레이션 성공 여부 확인 ($?는 이전 명령어의 종료 상태 코드입니다)
if [ $? -eq 0 ]; then
    echo "> DB 마이그레이션 성공적으로 완료."
else
    echo "> DB 마이그레이션 실패! 배포를 중단합니다."
    exit 1 # CodeDeploy에게 실패를 알리고 배포를 중단시킵니다.
fi