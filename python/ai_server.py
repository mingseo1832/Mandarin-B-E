import os
import re
import csv
import io
from datetime import datetime, timedelta
from collections import OrderedDict
from dotenv import load_dotenv, find_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Literal, Optional, Dict, Tuple
from openai import OpenAI

# ============================================================
# Pydantic 모델 정의 (기존 kakaotalk_text_test.py에서 가져옴)
# ============================================================

class EmojiStyle(BaseModel):
    frequency: Literal["High", "Medium", "Low", "None"] = Field(description="이모티콘이나 특수문자 사용 빈도")
    preferred_type: Literal["Text", "Graphic", "Mixed"] = Field(description="선호하는 유형 (Text: ㅠㅠ,ㅋㅋ / Graphic: 카톡이모티콘)")
    laugh_sound: str = Field(description="주된 웃음 소리 (예: ㅋㅋㅋ, ㅎㅎ, kkk, 푸하하)")


class SpeechStyleAnalysis(BaseModel):
    politeness_level: Literal["반말", "존댓말", "상호존대", "오락가락함"] = Field(description="대화 상대방에 대한 높임말 사용 여부")
    tone: str = Field(description="전반적인 말투의 분위기 (예: 시니컬한, 애교있는, 다급한, 논리적인)")
    common_endings: List[str] = Field(description="자주 사용하는 문장 종결어미 3~5개 (예: ~함, ~용, ~누, ~라고)")
    frequent_interjections: List[str] = Field(description="자주 사용하는 추임새나 감탄사 (예: 아 진짜, 솔까, ㄴㄴ)")
    emoji_usage: EmojiStyle = Field(description="이모티콘 및 특수문자 활용 분석")
    distinctive_habits: List[str] = Field(description="기타 식별 가능한 말투 습관 (띄어쓰기, 맞춤법 파괴 등)")
    sample_sentences: List[str] = Field(description="이 사람의 말투가 가장 잘 드러나는 실제 대화 문장 3개 추출")


class UserPersona(BaseModel):
    name: str = Field(description="사용자 이름")
    speech_style: SpeechStyleAnalysis


# ============================================================
# API 요청/응답 모델
# ============================================================

# [추가] /parse-info 요청을 위한 모델
class ParseRequest(BaseModel):
    text_content: str = Field(description="파싱할 카카오톡 대화 내용")

class AnalyzeRequest(BaseModel):
    text_content: str = Field(description="분석할 카카오톡 대화 내용")
    target_name: str = Field(description="분석 대상 인물 이름")
    period_days: int = Field(default=14, description="분석할 기간 (일 단위, 기본 14일)")
    start_date: Optional[str] = Field(default=None, description="시작일 (YYYY-MM-DD 형식, 종료일과 함께 사용)")
    end_date: Optional[str] = Field(default=None, description="종료일 (YYYY-MM-DD 형식, 시작일과 함께 사용)")
    buffer_days: int = Field(default=0, description="시작일 이전 버퍼 일수 (기본 0일)")


class ParseInfoResponse(BaseModel):
    format_type: str = Field(description="파일 형식 (windows/mac/ios/android)")
    total_days: int = Field(description="총 대화 일수")
    total_messages: int = Field(description="총 메시지 수")
    participants: List[str] = Field(description="참여자 목록")
    participant_count: int = Field(description="참여자 수")
    date_range: dict = Field(description="대화 기간")


class ChatRequest(BaseModel):
    persona: UserPersona = Field(description="대화에 사용할 페르소나 정보")
    user_message: str = Field(description="사용자 메시지")
    history: List[dict] = Field(default=[], description="이전 대화 내역")


class ChatResponse(BaseModel):
    reply: str = Field(description="생성된 응답")


class ReportRequest(BaseModel):
    chat_logs: List[dict] = Field(description="분석할 대화 로그")
    user_name: str = Field(description="사용자(본인) 이름")
    target_name: str = Field(description="페르소나 대상(상대방) 이름")


class ConversationScores(BaseModel):
    """대화 점수 상세"""
    overall_flow: int = Field(description="전체 대화 흐름 점수 (0-100)", ge=0, le=100)
    emotional_connection: int = Field(description="감정적 교감 점수 (0-100)", ge=0, le=100)
    interest_signal: int = Field(description="호감 신호 점수 (0-100)", ge=0, le=100)
    conversation_skill: int = Field(description="대화 스킬 점수 (0-100)", ge=0, le=100)
    timing_response: int = Field(description="타이밍/반응 적절성 점수 (0-100)", ge=0, le=100)


class ChatMessage(BaseModel):
    """채팅 메시지"""
    role: Literal["user", "target"] = Field(description="발화자 (user: 사용자, target: 페르소나)")
    content: str = Field(description="메시지 내용")


class HighlightMoment(BaseModel):
    """주요 순간 분석 - 대화 맥락 포함"""
    moment_type: Literal["positive", "negative", "neutral"] = Field(description="순간 유형")
    conversation: List[ChatMessage] = Field(
        description="해당 순간의 대화 흐름 (사용자와 페르소나 간 2-4개 메시지)"
    )
    analysis: str = Field(description="해당 대화에 대한 분석")
    suggestion: Optional[str] = Field(default=None, description="개선 제안 (부정적인 경우)")


class RomanceAnalysisReport(BaseModel):
    """연애 관점 대화 분석 보고서"""
    # 종합 점수
    total_score: int = Field(description="종합 호감도 점수 (0-100)", ge=0, le=100)
    score_grade: Literal["S", "A", "B", "C", "D", "F"] = Field(description="등급")
    
    # 상세 점수
    scores: ConversationScores = Field(description="상세 점수")
    
    # 대화 흐름 분석
    flow_direction: Literal["매우 긍정적", "긍정적", "중립", "부정적", "매우 부정적"] = Field(
        description="대화 흐름 방향"
    )
    flow_summary: str = Field(description="대화 흐름 요약 (2-3문장)")
    
    # 주요 순간들
    highlight_moments: List[HighlightMoment] = Field(description="주요 순간 분석 (최대 5개)")
    
    # 호감도 분석
    attraction_signals: List[str] = Field(description="상대방이 보인 호감 신호들")
    red_flags: List[str] = Field(description="주의해야 할 부정적 신호들")
    
    # 종합 피드백
    strengths: List[str] = Field(description="잘한 점 (최대 3개)")
    improvements: List[str] = Field(description="개선할 점 (최대 3개)")
    
    # 다음 대화 조언
    next_conversation_tips: List[str] = Field(description="다음 대화를 위한 조언 (최대 3개)")
    
    # 총평
    overall_assessment: str = Field(description="종합 평가 (3-5문장)")


class ReportResponse(BaseModel):
    report: RomanceAnalysisReport = Field(description="생성된 연애 분석 보고서")


# ============================================================
# 환경 설정 및 OpenAI 클라이언트 초기화
# ============================================================

_ = load_dotenv(find_dotenv())

API_KEY = os.environ.get("API_KEY")
DEFAULT_MODEL = os.environ.get("DEFAULT_MODEL", "gpt-5-nano")

client = OpenAI(api_key=API_KEY)

# ============================================================
# FastAPI 앱 생성
# ============================================================

app = FastAPI(
    title="카카오톡 페르소나 분석 API",
    description="카카오톡 대화 로그를 분석하여 페르소나를 추출하고, 해당 페르소나로 대화를 생성하는 API",
    version="1.0.0"
)


# ============================================================
# 카카오톡 파서 클래스 (윈도우/맥/iOS/안드로이드 지원)
# ============================================================

class KakaoTalkParser:
    """
    카카오톡 대화 파싱 및 날짜별 청킹 클래스
    
    지원 플랫폼:
    - Windows: [이름] [시간] 메시지 형식
    - Mac: CSV 파일 (Date,User,Message)
    - iOS: 2025. 10. 23. 오전 11:44, 이름 : 메시지 형식
    - Android: 2025년 10월 10일 오전 10:09, 이름 : 메시지 형식
    """
    
    # ===== 윈도우 패턴 =====
    # 날짜: --------------- 2025년 8월 14일 목요일 ---------------
    WINDOWS_DATE_PATTERN = re.compile(r'^-+ (\d{4})년 (\d{1,2})월 (\d{1,2})일.*-+$')
    # 메시지: [이재균] [오전 12:01] 사진
    WINDOWS_MESSAGE_PATTERN = re.compile(r'^\[([^\]]+)\]\s*\[([^\]]+)\]\s*(.*)$')
    
    # ===== iOS 패턴 =====
    # 날짜: 2025년 10월 23일 목요일
    IOS_DATE_PATTERN = re.compile(r'^(\d{4})년 (\d{1,2})월 (\d{1,2})일 \S+요일\s*$')
    # 메시지: 2025. 10. 23. 오전 11:44, 안도현 : 동방에 다 있을걸
    IOS_MESSAGE_PATTERN = re.compile(
        r'^(\d{4})\. (\d{1,2})\. (\d{1,2})\. (오전|오후) (\d{1,2}):(\d{2}),\s*([^:]+)\s*:\s*(.*)$'
    )
    
    # ===== 안드로이드 패턴 =====
    # 날짜+시간: 2025년 10월 10일 오전 10:09
    ANDROID_DATE_PATTERN = re.compile(r'^(\d{4})년 (\d{1,2})월 (\d{1,2})일 (오전|오후) (\d{1,2}):(\d{2})\s*$')
    # 메시지: 2025년 10월 10일 오전 10:09, 이재균 : ㅎㅇ
    ANDROID_MESSAGE_PATTERN = re.compile(
        r'^(\d{4})년 (\d{1,2})월 (\d{1,2})일 (오전|오후) (\d{1,2}):(\d{2}),\s*([^:]+)\s*:\s*(.*)$'
    )
    
    # ===== 맥 CSV 패턴 =====
    # 헤더: Date,User,Message
    # 데이터: 2025-10-10 10:09:20,"이재균","ㅎㅇ"
    MAC_CSV_HEADER = "Date,User,Message"
    
    def __init__(self, text_content: str):
        self.raw_text = text_content
        self.daily_chats: Dict[datetime, List[dict]] = OrderedDict()
        self.format_type: str = "unknown"  # "windows", "mac", "ios", "android"
        self._detect_format()
        self._parse()
    
    def _detect_format(self):
        """파일 형식 자동 감지"""
        lines = self.raw_text.split('\n')[:20]  # 처음 20줄만 확인
        
        for line in lines:
            line_stripped = line.strip()
            
            # 맥 CSV 감지 (헤더 확인)
            if line_stripped == self.MAC_CSV_HEADER or line_stripped.startswith("Date,User,"):
                self.format_type = "mac"
                return
            
            # 윈도우 감지: --------------- 2025년 8월 14일 목요일 ---------------
            if self.WINDOWS_DATE_PATTERN.match(line_stripped):
                self.format_type = "windows"
                return
            
            # iOS 감지: 2025. 10. 23. 오전 11:44, 안도현 : 메시지
            if self.IOS_MESSAGE_PATTERN.match(line_stripped):
                self.format_type = "ios"
                return
            
            # 안드로이드 감지: 2025년 10월 10일 오전 10:09, 이름 : 메시지
            if self.ANDROID_MESSAGE_PATTERN.match(line_stripped):
                self.format_type = "android"
                return
        
        # 기본값은 윈도우
        self.format_type = "windows"
    
    def _parse(self):
        """전체 텍스트를 파싱하여 날짜별로 구조화"""
        if self.format_type == "mac":
            self._parse_mac()
        elif self.format_type == "ios":
            self._parse_ios()
        elif self.format_type == "android":
            self._parse_android()
        else:  # windows
            self._parse_windows()
    
    def _convert_ampm_hour(self, ampm: str, hour: int) -> int:
        """오전/오후를 24시간 형식으로 변환"""
        if ampm == "오후" and hour != 12:
            return hour + 12
        elif ampm == "오전" and hour == 12:
            return 0
        return hour
    
    def _format_time_str(self, hour: int, minute: int) -> str:
        """시간을 오전/오후 형식 문자열로 변환"""
        ampm = "오전" if hour < 12 else "오후"
        display_hour = hour % 12 or 12
        return f"{ampm} {display_hour}:{minute:02d}"
    
    def _add_message(self, date: datetime, sender: str, time_str: str, content: str):
        """메시지를 daily_chats에 추가"""
        date_only = datetime(date.year, date.month, date.day)
        if date_only not in self.daily_chats:
            self.daily_chats[date_only] = []
        self.daily_chats[date_only].append({
            "sender": sender.strip(),
            "time": time_str,
            "content": content.strip()
        })
    
    def _parse_windows(self):
        """윈도우 버전 파싱: [이름] [시간] 메시지"""
        lines = self.raw_text.split('\n')
        current_date = None
        current_messages = []
        
        for line in lines:
            line = line.strip()
            
            # 날짜 라인 체크
            date_match = self.WINDOWS_DATE_PATTERN.match(line)
            if date_match:
                # 이전 날짜 데이터 저장
                if current_date and current_messages:
                    self.daily_chats[current_date] = current_messages
                
                year, month, day = date_match.groups()
                current_date = datetime(int(year), int(month), int(day))
                current_messages = []
                continue
            
            # 메시지 파싱
            if current_date:
                msg_match = self.WINDOWS_MESSAGE_PATTERN.match(line)
                if msg_match:
                    current_messages.append({
                        "sender": msg_match.group(1),
                        "time": msg_match.group(2),
                        "content": msg_match.group(3)
                    })
                elif line and not line.startswith('메시지가 삭제'):
                    # 연속 메시지 또는 시스템 메시지
                    current_messages.append({
                        "sender": None,
                        "time": None,
                        "content": line
                    })
        
        # 마지막 날짜 저장
        if current_date and current_messages:
            self.daily_chats[current_date] = current_messages
    
    def _parse_mac(self):
        """맥 CSV 버전 파싱: Date,User,Message"""
        try:
            reader = csv.DictReader(io.StringIO(self.raw_text))
            
            for row in reader:
                try:
                    # Date: 2025-10-10 10:09:20
                    date_str = row.get('Date', '')
                    user = row.get('User', '')
                    message = row.get('Message', '')
                    
                    if not date_str or not user:
                        continue
                    
                    # 날짜/시간 파싱
                    parsed_dt = datetime.strptime(date_str, '%Y-%m-%d %H:%M:%S')
                    time_str = self._format_time_str(parsed_dt.hour, parsed_dt.minute)
                    
                    self._add_message(parsed_dt, user, time_str, message)
                    
                except (ValueError, KeyError):
                    continue
                    
        except csv.Error:
            # CSV 파싱 실패 시 빈 상태 유지
            pass
    
    def _parse_ios(self):
        """iOS 버전 파싱: 2025. 10. 23. 오전 11:44, 안도현 : 메시지"""
        lines = self.raw_text.split('\n')
        
        for line in lines:
            line = line.strip()
            
            # 메시지 패턴 매칭
            msg_match = self.IOS_MESSAGE_PATTERN.match(line)
            if msg_match:
                year, month, day, ampm, hour, minute, sender, content = msg_match.groups()
                
                try:
                    hour_24 = self._convert_ampm_hour(ampm, int(hour))
                    parsed_dt = datetime(int(year), int(month), int(day), hour_24, int(minute))
                    time_str = self._format_time_str(hour_24, int(minute))
                    
                    self._add_message(parsed_dt, sender, time_str, content)
                except ValueError:
                    continue
    
    def _parse_android(self):
        """안드로이드 버전 파싱: 2025년 10월 10일 오전 10:09, 이재균 : 메시지"""
        lines = self.raw_text.split('\n')
        
        for line in lines:
            line = line.strip()
            
            # 메시지 패턴 매칭
            msg_match = self.ANDROID_MESSAGE_PATTERN.match(line)
            if msg_match:
                year, month, day, ampm, hour, minute, sender, content = msg_match.groups()
                
                try:
                    hour_24 = self._convert_ampm_hour(ampm, int(hour))
                    parsed_dt = datetime(int(year), int(month), int(day), hour_24, int(minute))
                    time_str = self._format_time_str(hour_24, int(minute))
                    
                    self._add_message(parsed_dt, sender, time_str, content)
                except ValueError:
                    continue
    
    def get_date_range(self) -> Tuple[Optional[datetime], Optional[datetime]]:
        """대화의 시작일과 종료일 반환"""
        if not self.daily_chats:
            return None, None
        dates = list(self.daily_chats.keys())
        return min(dates), max(dates)
    
    def filter_by_period(
        self, 
        days: int = 7, 
        from_date: Optional[datetime] = None,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
        buffer_days: int = 0
    ) -> Dict[datetime, List[dict]]:
        """특정 기간의 대화만 필터링
        
        두 가지 방식 지원:
        1. from_date + days: 지정일로부터 N일 전까지 필터링 (기존 방식)
        2. start_date + end_date: 시작일 - buffer_days부터 종료일까지 필터링 (새 방식)
        
        Args:
            days: 필터링할 일수 (기존 방식에서 사용)
            from_date: 기준일 (기존 방식에서 사용, None이면 최신 날짜)
            start_date: 시작일 (새 방식에서 사용)
            end_date: 종료일 (새 방식에서 사용)
            buffer_days: 시작일 이전 버퍼 일수 (새 방식에서 사용, 기본 0일)
        
        Returns:
            필터링된 날짜별 메시지 딕셔너리
        
        Note:
            start_date와 end_date가 모두 제공되면 새 방식 우선 적용
        """
        if not self.daily_chats:
            return {}
        
        # 새 방식: start_date와 end_date가 모두 제공된 경우
        if start_date is not None and end_date is not None:
            actual_start = start_date - timedelta(days=buffer_days)
            return OrderedDict(
                (date, messages) 
                for date, messages in self.daily_chats.items()
                if actual_start <= date <= end_date
            )
        
        # 기존 방식: from_date로부터 days일 전까지
        if from_date is None:
            from_date = max(self.daily_chats.keys())
        
        calc_start_date = from_date - timedelta(days=days)
        
        return OrderedDict(
            (date, messages) 
            for date, messages in self.daily_chats.items()
            if calc_start_date <= date <= from_date
        )
    
    def filter_by_sender(self, sender_name: str, data: Optional[Dict] = None) -> Dict[datetime, List[dict]]:
        """특정 발신자의 메시지만 필터링"""
        source = data or self.daily_chats
        filtered = OrderedDict()
        
        for date, messages in source.items():
            sender_messages = [
                msg for msg in messages 
                if msg.get("sender") and sender_name in msg["sender"]
            ]
            if sender_messages:
                filtered[date] = sender_messages
        
        return filtered
    
    def to_text(self, data: Optional[Dict] = None) -> str:
        """구조화된 데이터를 다시 텍스트로 변환"""
        source = data or self.daily_chats
        result = []
        
        for date, messages in source.items():
            result.append(f"--- {date.strftime('%Y년 %m월 %d일')} ---")
            for msg in messages:
                if msg["sender"]:
                    result.append(f"[{msg['sender']}] [{msg['time']}] {msg['content']}")
                else:
                    result.append(msg['content'])
        
        return '\n'.join(result)
    
    def get_participants(self) -> List[str]:
        """대화 참여자 목록 반환"""
        senders = set()
        for messages in self.daily_chats.values():
            for msg in messages:
                if msg.get("sender"):
                    senders.add(msg["sender"])
        return sorted(list(senders))
    
    def get_statistics(self) -> dict:
        """대화 통계 반환"""
        total_messages = sum(len(msgs) for msgs in self.daily_chats.values())
        participants = self.get_participants()
        start_date, end_date = self.get_date_range()
        
        return {
            "format_type": self.format_type,
            "total_days": len(self.daily_chats),
            "total_messages": total_messages,
            "participants": participants,
            "participant_count": len(participants),
            "date_range": {
                "start": start_date.isoformat() if start_date else None,
                "end": end_date.isoformat() if end_date else None
            }
        }


def preprocess_kakao_text(
    text_content: str, 
    target_name: str,
    period_days: int = 14,
    max_chars: int = 50000,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    buffer_days: int = 0
) -> Tuple[str, dict]:
    """
    카카오톡 텍스트 전처리 (기간 필터링, 전체 대화 맥락 유지)
    
    Args:
        text_content: 카카오톡 대화 텍스트
        target_name: 분석 대상 인물 이름 (존재 여부 확인용)
        period_days: 필터링할 일수 (기존 방식에서 사용)
        max_chars: 최대 문자 수 제한
        start_date: 시작일 (새 방식에서 사용)
        end_date: 종료일 (새 방식에서 사용)
        buffer_days: 시작일 이전 버퍼 일수 (새 방식에서 사용, 기본 0일)
    
    Returns:
        (전처리된 텍스트, 통계 정보)
    
    Note:
        - start_date와 end_date가 모두 제공되면 해당 기간으로 필터링,
          그렇지 않으면 최신 날짜 기준 period_days일 전까지 필터링
        - 전체 대화 맥락을 유지하여 GPT가 대화 흐름을 이해하면서
          특정 인물의 말투를 분석할 수 있도록 함
    """
    parser = KakaoTalkParser(text_content)
    stats = parser.get_statistics()
    
    # 1. 기간 필터링
    if start_date is not None and end_date is not None:
        # 새 방식: 시작일 - buffer_days부터 종료일까지
        period_data = parser.filter_by_period(
            start_date=start_date,
            end_date=end_date,
            buffer_days=buffer_days
        )
        stats["filter_mode"] = "date_range"
        stats["start_date"] = start_date.isoformat()
        stats["end_date"] = end_date.isoformat()
        stats["buffer_days"] = buffer_days
    else:
        # 기존 방식: 최근 N일
        period_data = parser.filter_by_period(days=period_days)
        stats["filter_mode"] = "recent_days"
        stats["filtered_period_days"] = period_days
    
    # 2. 타겟 인물이 해당 기간에 존재하는지 확인
    target_messages = parser.filter_by_sender(target_name, period_data)
    if not target_messages:
        stats["target_found"] = False
    else:
        stats["target_found"] = True
        stats["target_message_count"] = sum(len(msgs) for msgs in target_messages.values())
    
    # 3. 전체 대화 텍스트 변환 (맥락 유지를 위해 특정 인물 필터링 제거)
    result_text = parser.to_text(period_data)
    
    # 4. 최대 길이 제한 (최신 대화 우선)
    if len(result_text) > max_chars:
        result_text = result_text[-max_chars:]
    
    # 통계에 필터링 정보 추가
    stats["target_name"] = target_name
    stats["filtered_char_count"] = len(result_text)
    
    return result_text, stats


# ============================================================
# 유틸리티 함수
# ============================================================

def build_system_prompt(text_content: str) -> str:
    """동적으로 시스템 프롬프트를 생성합니다."""
    return f"""
당신은 카카오톡 대화 로그 전문 분석가입니다.
제공된 [대화 내용]을 정밀 분석하여, 대화에 참여하는 인물이 누가 있는지 파악한 뒤, 사용자가 지정한 특정 인물의 '페르소나(Persona)'를 추출해야 합니다.

[대화 내용]
{text_content[-15000:]}

[분석 지침]
1. 'preferred_type' 판단 기준: 
   - 텍스트(ㅠㅠ, ㅋㅋ, ^^) 위주면 'Text'
   - '이모티콘' 텍스트가 자주 보이면 'Graphic'으로 판단하세요.
2. 'tone'은 추상적이지 않고 구체적인 형용사로 기술하세요.
3. 사용자가 특정 인물을 지목하면, 그 사람의 발화만 필터링하여 분석하세요.
"""


def extract_persona(target_name: str, system_prompt: str) -> Optional[UserPersona]:
    """OpenAI API를 사용하여 페르소나를 추출합니다."""
    try:
        response = client.responses.parse(
            model=DEFAULT_MODEL,
            instructions=system_prompt,
            input=f"'{target_name}'의 페르소나를 분석해주세요.",
            text_format=UserPersona,
        )
        return response.output_parsed
    except Exception as e:
        print(f"페르소나 추출 오류: {e}")
        return None


def generate_reply(persona: UserPersona, user_message: str, history: List[dict]) -> str:
    """페르소나 정보를 기반으로 대화 응답을 생성합니다."""
    
    # 페르소나 정보를 시스템 프롬프트로 변환
    persona_prompt = f"""
당신은 '{persona.name}'이라는 사람의 말투를 완벽하게 모방해야 합니다.

[말투 특성]
- 존대 여부: {persona.speech_style.politeness_level}
- 말투 톤: {persona.speech_style.tone}
- 자주 쓰는 종결어미: {', '.join(persona.speech_style.common_endings)}
- 자주 쓰는 추임새: {', '.join(persona.speech_style.frequent_interjections)}
- 웃음 소리: {persona.speech_style.emoji_usage.laugh_sound}
- 이모티콘 빈도: {persona.speech_style.emoji_usage.frequency}
- 특이한 습관: {', '.join(persona.speech_style.distinctive_habits)}

[예시 문장]
{chr(10).join(f'- {s}' for s in persona.speech_style.sample_sentences)}

위 특성을 반영하여 '{persona.name}'처럼 자연스럽게 대화하세요.
절대 캐릭터에서 벗어나지 마세요.
"""
    
    messages = [{"role": "system", "content": persona_prompt}]
    
    # 이전 대화 내역 추가
    for msg in history:
        messages.append(msg)
    
    # 현재 사용자 메시지 추가
    messages.append({"role": "user", "content": user_message})
    
    try:
        response = client.chat.completions.create(
            model=DEFAULT_MODEL,
            messages=messages,
        )
        return response.choices[0].message.content
    except Exception as e:
        print(f"응답 생성 오류: {e}")
        return "죄송합니다. 응답 생성 중 오류가 발생했습니다."


def analyze_chat_performance(chat_logs: List[dict], user_name: str, target_name: str) -> Optional[RomanceAnalysisReport]:
    """대화 로그를 분석하여 연애 관점의 보고서를 생성합니다.
    
    Args:
        chat_logs: 대화 로그 리스트 (role, content 포함)
        user_name: 사용자(본인) 이름
        target_name: 페르소나 대상(상대방) 이름
    
    Returns:
        RomanceAnalysisReport: 구조화된 연애 분석 보고서
    """
    
    logs_text = "\n".join([
        f"{log.get('role', 'unknown')}: {log.get('content', '')}" 
        for log in chat_logs
    ])
    
    system_prompt = f"""당신은 연애 심리 전문가이자 대화 코칭 전문가입니다.
남녀 간의 대화를 분석하여 연애적 관점에서 평가하고 조언을 제공합니다.

분석 시 다음 관점을 중점적으로 고려하세요:
1. 대화의 감정적 흐름 (긍정적/부정적 방향성)
2. 호감을 살 수 있는 발언 vs 호감을 떨어뜨리는 발언
3. 상대방의 반응에서 읽히는 관심도/호감도 신호
4. 대화 타이밍, 리액션의 적절성
5. 밀당, 유머, 공감 등 연애 대화 스킬

점수 기준:
- 90-100 (S등급): 완벽한 대화, 확실한 호감 획득
- 80-89 (A등급): 매우 좋은 대화, 호감 상승
- 70-79 (B등급): 괜찮은 대화, 긍정적 인상
- 60-69 (C등급): 평범한 대화, 특별한 인상 없음
- 50-59 (D등급): 아쉬운 대화, 일부 개선 필요
- 0-49 (F등급): 부정적 대화, 많은 개선 필요

[highlight_moments 작성 규칙]
- conversation 필드에는 해당 순간의 대화 흐름을 채팅창처럼 2-4개 메시지로 구성
- role은 "user"(사용자: {user_name}) 또는 "target"(상대방: {target_name})
- 대화의 맥락을 이해할 수 있도록 사용자 발언과 상대방 응답을 함께 포함
- 예시: [
    {{"role": "user", "content": "오늘 뭐해?"}},
    {{"role": "target", "content": "그냥 집에~ 왜?"}},
    {{"role": "user", "content": "심심하면 나랑 밥먹을래?"}}
  ]

사용자({user_name})의 발언을 중심으로 분석하고, 
상대방({target_name})에게 어떤 인상을 주었을지 평가해주세요."""

    analysis_prompt = f"""다음 대화를 연애 관점에서 분석해주세요.

[참여자 정보]
- 사용자(분석 대상): {user_name}
- 상대방(페르소나): {target_name}

[대화 로그]
{logs_text}

위 대화에서 {user_name}의 발언이 {target_name}에게 호감을 살 만 했는지,
대화가 전반적으로 긍정적인 방향으로 흘러갔는지 분석해주세요."""
    
    try:
        response = client.responses.parse(
            model=DEFAULT_MODEL,
            instructions=system_prompt,
            input=analysis_prompt,
            text_format=RomanceAnalysisReport,
        )
        return response.output_parsed
    except Exception as e:
        print(f"보고서 생성 오류: {e}")
        return None


# ============================================================
# API 엔드포인트
# ============================================================

@app.get("/")
def root():
    """API 상태 확인"""
    return {"status": "running", "message": "카카오톡 페르소나 분석 API"}


@app.post("/parse-info", response_model=ParseInfoResponse)
def get_parse_info(req: ParseRequest):
    """
    카카오톡 대화 내용을 파싱하여 기본 정보를 반환합니다.
    (참여자 목록, 대화 기간 등)
    """
    if not req.text_content.strip():
        raise HTTPException(status_code=400, detail="대화 내용이 비어있습니다.")
    
    parser = KakaoTalkParser(req.text_content)
    stats = parser.get_statistics()
    
    return ParseInfoResponse(**stats)


@app.post("/analyze", response_model=UserPersona)
def analyze_character(req: AnalyzeRequest):
    """
    카카오톡 대화 내용에서 특정 인물의 페르소나를 추출합니다.
    
    - **text_content**: 분석할 카카오톡 대화 텍스트
    - **target_name**: 분석 대상 인물 이름
    - **period_days**: 분석할 기간 (기본 14일, start_date/end_date 미지정 시 사용)
    - **start_date**: 시작일 (YYYY-MM-DD 형식, end_date와 함께 사용)
    - **end_date**: 종료일 (YYYY-MM-DD 형식, start_date와 함께 사용)
    - **buffer_days**: 시작일 이전 버퍼 일수 (기본 0일)
    """
    if not req.text_content.strip():
        raise HTTPException(status_code=400, detail="대화 내용이 비어있습니다.")
    
    if not req.target_name.strip():
        raise HTTPException(status_code=400, detail="분석 대상 이름이 비어있습니다.")
    
    # 날짜 문자열을 datetime으로 변환
    parsed_start_date = None
    parsed_end_date = None
    
    if req.start_date and req.end_date:
        try:
            parsed_start_date = datetime.strptime(req.start_date, "%Y-%m-%d")
            parsed_end_date = datetime.strptime(req.end_date, "%Y-%m-%d")
        except ValueError:
            raise HTTPException(
                status_code=400, 
                detail="날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요."
            )
        
        if parsed_start_date > parsed_end_date:
            raise HTTPException(
                status_code=400,
                detail="시작일이 종료일보다 늦을 수 없습니다."
            )
    elif req.start_date or req.end_date:
        raise HTTPException(
            status_code=400,
            detail="시작일과 종료일을 모두 입력하거나, 둘 다 입력하지 마세요."
        )
    
    # 전처리: 기간 필터링 (전체 대화 맥락 유지)
    processed_text, stats = preprocess_kakao_text(
        text_content=req.text_content,
        target_name=req.target_name,
        period_days=req.period_days,
        start_date=parsed_start_date,
        end_date=parsed_end_date,
        buffer_days=req.buffer_days
    )
    
    if not processed_text.strip():
        raise HTTPException(
            status_code=400, 
            detail="해당 기간에 대화 내용이 없습니다."
        )
    
    if not stats.get("target_found", False):
        raise HTTPException(
            status_code=400, 
            detail=f"'{req.target_name}'의 메시지를 찾을 수 없습니다. 참여자: {stats.get('participants', [])}"
        )
    
    print(f"[분석] 대상: {req.target_name}, 타겟 메시지 수: {stats.get('target_message_count', 0)}, 전체 문자수: {stats['filtered_char_count']}")
    
    system_prompt = build_system_prompt(processed_text)
    persona = extract_persona(req.target_name, system_prompt)
    
    if not persona:
        raise HTTPException(status_code=500, detail="페르소나 추출에 실패했습니다.")
    
    return persona


@app.post("/chat", response_model=ChatResponse)
def chat_with_character(req: ChatRequest):
    """
    추출된 페르소나를 기반으로 대화 응답을 생성합니다.
    
    - **persona**: 사용할 페르소나 정보 (UserPersona 객체)
    - **user_message**: 사용자가 보내는 메시지
    - **history**: 이전 대화 내역 (선택사항)
    """
    if not req.user_message.strip():
        raise HTTPException(status_code=400, detail="메시지가 비어있습니다.")
    
    reply = generate_reply(req.persona, req.user_message, req.history)
    return ChatResponse(reply=reply)


@app.post("/report", response_model=ReportResponse)
def create_report(req: ReportRequest):
    """
    대화 로그를 연애 관점에서 분석하여 보고서를 생성합니다.
    
    - **chat_logs**: 분석할 대화 로그 리스트 (role: user/assistant, content: 메시지)
    - **user_name**: 사용자(본인) 이름
    - **target_name**: 페르소나 대상(상대방) 이름
    
    Returns:
        종합 점수, 상세 점수, 대화 흐름 분석, 호감도 분석, 개선점 등을 포함한 보고서
    """
    if not req.chat_logs:
        raise HTTPException(status_code=400, detail="대화 로그가 비어있습니다.")
    
    if not req.user_name.strip():
        raise HTTPException(status_code=400, detail="사용자 이름이 비어있습니다.")
    
    if not req.target_name.strip():
        raise HTTPException(status_code=400, detail="상대방 이름이 비어있습니다.")
    
    report = analyze_chat_performance(req.chat_logs, req.user_name, req.target_name)
    
    if not report:
        raise HTTPException(status_code=500, detail="보고서 생성에 실패했습니다.")
    
    return ReportResponse(report=report)


# ============================================================
# 서버 실행 (직접 실행 시)
# ============================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("ai_server:app", host="0.0.0.0", port=8000, reload=True)

