#!/bin/bash

# 배포 경로 및 JAR 파일 이름 설정
DEPLOY_PATH="/home/ec2-user/app"
JAR_NAME=$(ls -tr $DEPLOY_PATH/*.jar | tail -n 1)

# 로그 파일을 저장할 경로 (필요 시 변경 가능)
LOG_PATH="$DEPLOY_PATH/application.log"

echo "> $JAR_NAME 새로운 애플리케이션 시작"

# nohup을 사용하여 백그라운드에서 실행 (SSH 세션 종료 후에도 유지)
# 출력(STDOUT)과 에러(STDERR)를 로그 파일에 기록합니다.
nohup java -jar $JAR_NAME --spring.profiles.active=prod > $LOG_PATH 2>&1 &

echo "> 백그라운드에서 서버 시작 명령 완료."