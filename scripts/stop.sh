#!/bin/bash

# 💡 프로젝트의 artifact ID를 사용하여 프로세스를 찾습니다. (버전 변경 시에도 동작)
APP_ARTIFACT_ID="mandarin_backend"

# 실행 중인 프로세스 ID(PID)를 찾습니다.
# 'mandarin_backend' 문자열을 포함하는 모든 자바 프로세스를 찾습니다.
CURRENT_PID=$(pgrep -f $APP_ARTIFACT_ID)

if [ -z "$CURRENT_PID" ]; then
    echo "> 현재 실행 중인 애플리케이션($APP_ARTIFACT_ID)이 없습니다. 종료하지 않습니다."
else
    echo "> 실행 중인 PID: $CURRENT_PID"
    kill -15 $CURRENT_PID
    echo "> $CURRENT_PID 프로세스 종료 완료."
fi