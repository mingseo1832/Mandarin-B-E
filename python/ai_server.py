import os
import json
from pathlib import Path
from dotenv import load_dotenv, find_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Literal, Optional, Dict, Any
from openai import OpenAI

# ============================================================
# Lovetype 데이터 로드
# ============================================================

def load_lovetype_data() -> Dict[str, Any]:
    """lovetype_summary.json 파일을 로드합니다."""
    # Python 스크립트 위치 기준으로 상대 경로 계산
    current_dir = Path(__file__).parent
    lovetype_path = current_dir / "lovetype_summary.json"
    
    try:
        with open(lovetype_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"[WARNING] lovetype_summary.json not found at {lovetype_path}")
        return {}
    except json.JSONDecodeError as e:
        print(f"[ERROR] Failed to parse lovetype_summary.json: {e}")
        return {}

# 앱 시작 시 lovetype 데이터 로드
LOVETYPE_DATA = load_lovetype_data()

# ============================================================
# 상수 정의
# ============================================================

# 최대 문자 수 제한 기본값 (Java와 동일)
DEFAULT_MAX_CHARS = 150000

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


class ReactionTrigger(BaseModel):
    """사용자의 특정 말에 대한 반응 트리거"""
    keyword: str = Field(description="갈등 상황을 한 단어로 표현한 키워드, 캐릭터 리포트에서 사용함.(예: '비난', '무관심', '짜증', '비아냥', '회피')")
    trigger: str = Field(description="사용자가 한 말/행동/주제 (예: '칭찬', '약속 변경', '애정 표현')")
    reaction: str = Field(description="그에 대한 상대방의 반응 패턴 (예: '기분 좋아하며 이모티콘 많이 사용', '짜증내며 단답으로 변함')")
    cause: str = Field(description="갈등이 발생한 구체적인 원인 (예: '공감이 부족하고 지속적으로 비난하여 감정이 상함', '비아냥대는 말투를 사용하여 불편함을 느끼게 함', '갈등을 해결하려 하지 않고 회피하거나 변명으로 무마하려고 함')")
    solution: str = Field(description="갈등 상황을 해결하기 위한 구체적인 조언. 갈등 원인을 고려하여 제시")
    example: str = Field(description="실제 대화에서 해당 반응이 나타난 예시 문장")
    danger_level: int = Field(description="갈등 상황의 위험도 점수(0-100). 관계에 미치는 부정적 영향의 심각도를 평가. 점수가 높을수록 더 부정적인 영향", ge=0, le=100)


class ReactionAnalysis(BaseModel):
    """긍정/부정 반응 분석"""
    positive_triggers: List[ReactionTrigger] = Field(
        description="사용자의 어떤 말/행동에 긍정적으로 반응했는지 TOP 3",
        min_length=1,
        max_length=3
    )
    negative_triggers: List[ReactionTrigger] = Field(
        description="사용자의 어떤 말/행동에 부정적으로 반응했는지 TOP 3",
        min_length=1,
        max_length=3
    )


class UserPersona(BaseModel):
    name: str = Field(description="사용자 이름")
    speech_style: SpeechStyleAnalysis
    reaction_patterns: ReactionAnalysis = Field(description="사용자 말에 대한 긍정/부정 반응 패턴 분석")


# ============================================================
# API 요청/응답 모델
# ============================================================
class AnalyzeRequest(BaseModel):
    text_content: str = Field(description="분석할 카카오톡 대화 내용")
    target_name: str = Field(description="분석 대상 인물 이름")

class SimulationContext(BaseModel):
    """시뮬레이션 컨텍스트 정보"""
    character_age: int = Field(description="캐릭터 나이")
    relation_type: int = Field(description="관계 타입 (0: 썸, 1: 연애, 2: 이별)")
    love_type: int = Field(default=16, description="연애 타입 (0~15, 16=미설정)")
    history_sum: Optional[str] = Field(default=None, description="사용자와 상대간의 이야기 요약")
    purpose: Literal["FUTURE", "PAST"] = Field(description="시뮬레이션 목적")
    category: str = Field(description="구체적인 상황 카테고리")
    meet_date: Optional[str] = Field(default=None, description="만난 날짜")


class ChatRequest(BaseModel):
    persona: UserPersona = Field(description="대화에 사용할 페르소나 정보")
    user_message: str = Field(description="사용자 메시지")
    history: List[dict] = Field(default=[], description="이전 대화 내역")
    simulation_context: Optional[SimulationContext] = Field(default=None, description="시뮬레이션 컨텍스트 정보")


class ChatResponse(BaseModel):
    reply: str = Field(description="생성된 응답")


class ReportRequest(BaseModel):
    chat_logs: List[dict] = Field(description="분석할 대화 로그")
    user_name: str = Field(description="사용자(본인) 이름")
    target_name: str = Field(description="페르소나 대상(상대방) 이름")
    scenario_type: Literal["FUTURE", "PAST"] = Field(description="시나리오 유형 (FUTURE: 미래 시뮬레이션, PAST: 과거 후회 시뮬레이션)")


class HistorySumRequest(BaseModel):
    history: str = Field(description="요약할 히스토리 내용")
    character_name: str = Field(description="캐릭터 이름 (요약 시 참고용)")


class HistorySumResponse(BaseModel):
    summary: str = Field(description="요약된 히스토리")


class KeyConversation(BaseModel):
    """점수 산정에 영향을 준 주요 대화"""
    role: Literal["user", "assistant"] = Field(description="발화자 (user: 사용자, assistant: 페르소나)")
    content: str = Field(description="메시지 내용")


class MetricScore(BaseModel):
    """개별 평가 지표 점수"""
    code: str = Field(description="지표 코드 (ECI, EVR, CCS 또는 RRI, EEQI, RPS)")
    name: str = Field(description="지표 이름")
    score: int = Field(description="점수 (0-100)", ge=0, le=100)
    reason: str = Field(description="점수 산정 이유 (한 문장)")
    key_conversations: List[KeyConversation] = Field(
        description="이 점수 산정에 주요 영향을 준 대화 내역 (2-4개 메시지)"
    )


class ScenarioScores(BaseModel):
    """시나리오별 평가 점수"""
    metric_1: MetricScore = Field(description="첫 번째 지표 (ECI 또는 RRI)")
    metric_2: MetricScore = Field(description="두 번째 지표 (EVR 또는 EEQI)")
    metric_3: MetricScore = Field(description="세 번째 지표 (CCS 또는 RPS)")


class ReportContent(BaseModel):
    """리포트 상세 내용"""
    analysis: str = Field(description="전반적인 대화 흐름과 사용자의 태도에 대한 상세 분석 (200자 내외)")
    feedback: str = Field(description="더 나은 결과를 위해 사용자에게 주는 구체적인 조언")
    overall_rating: int = Field(description="전체 리포트 점수의 평균값 (0-100)", ge=0, le=100)


class SimulationReport(BaseModel):
    """시뮬레이션 대화 분석 보고서"""
    summary: str = Field(description="대화 내용에 대한 3줄 요약")
    scenario_type: Literal["FUTURE", "PAST"] = Field(description="시나리오 유형")
    scores: ScenarioScores = Field(description="시나리오별 평가 점수")
    report: ReportContent = Field(description="리포트 상세 내용")


class ReportResponse(BaseModel):
    report: SimulationReport = Field(description="생성된 시뮬레이션 분석 보고서")


class ExtractNegativeTriggersRequest(BaseModel):
    text_content: str = Field(description="전체 대화 텍스트 (가장 최근부터 maxChars만큼)")
    user_name: str = Field(description="사용자(본인) 이름")
    target_name: str = Field(description="상대방(분석 대상) 이름")
    max_chars: int = Field(default=DEFAULT_MAX_CHARS, description="최대 문자 수")

class ExtractNegativeTriggersResponse(BaseModel):
    negative_triggers: List[ReactionTrigger] = Field(description="부정적 반응 트리거 TOP 3")


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
# 유틸리티 함수
# ============================================================

def build_system_prompt(text_content: str) -> str:
    """동적으로 시스템 프롬프트를 생성합니다."""
    return f"""
당신은 카카오톡 대화 로그 전문 분석가입니다.
제공된 [대화 내용]을 정밀 분석하여, 대화에 참여하는 인물이 누가 있는지 파악한 뒤, 사용자가 지정한 특정 인물의 '페르소나(Persona)'를 추출해야 합니다.

[대화 내용]
{text_content[-DEFAULT_MAX_CHARS:]}

[분석 지침]
1. 'preferred_type' 판단 기준: 
   - 텍스트(ㅠㅠ, ㅋㅋ, ^^) 위주면 'Text'
   - '이모티콘' 텍스트가 자주 보이면 'Graphic'으로 판단하세요.
2. 'tone'은 추상적이지 않고 구체적인 형용사로 기술하세요.
3. 사용자가 특정 인물을 지목하면, 그 사람의 발화를 중심으로 상대방의 발화를 포함하여 전체 대화 맥락을 고려하여 분석하세요.
    - 말투 분석(speech_style)은 사용자가 지목한 특정 인물의 말투만 분석하는 것이지만, 어떤 대화 맥락에서 나타난 말투인지를 고려하세요.
    - 평소 speech_style과 갈등 상황에서의 speech_style가 다른 경우가 있을 수 있습니다. 말투와 맥락을 연결하여 speech_style를 분석하세요.
    - 특히 tone, common_endings, emoji_usage는 맥락에 따른 변화가 매우 중요합니다.
    - [예시]
    - 평소 말투 : 존대말, 애교, 고마움 표현 등
    - 갈등 상황 말투 : 단답, 짜증, 서운함 표현, 읽씹 등

[반응 패턴 분석 지침 - 매우 중요!]
4. 'reaction_patterns'는 **분석 대상 인물**이 상대방(대화 상대)의 말/행동에 어떻게 반응했는지를 분석합니다.
    **사용자가 지정한 인물의 반응**입니다. 사용자의 반응을 분석하는 것으로 헷갈리거나 혼동해서는 안됩니다.
   **중요: 반응 패턴을 분석할 때는 반드시 전체 대화 맥락을 유지하세요.**
   - 상대방의 발화와 그 직후 분석 대상의 반응을 함께 분석해야 합니다.
   - 대화의 시간 순서를 고려하여, 어떤 말에 대한 반응인지 명확히 파악하세요.
5. 'positive_triggers': 상대방이 어떤 말을 했을 때 분석 대상이 기분 좋아했는지 TOP 3를 추출하세요.
   - 예: 칭찬, 애정 표현, 관심 표현, 공감, 선물/이벤트 제안, 약속 제안 등
   - 실제 대화에서 긍정적 반응(이모티콘 많아짐, 말이 길어짐, 애교, 고마움 표현 등)이 나타난 맥락을 찾으세요.
   - **중요: 상대방의 발화와 그에 대한 분석 대상의 반응을 함께 분석하여, 어떤 맥락에서 긍정적으로 반응했는지 파악하세요.**
6. 'negative_triggers': 상대방이 어떤 말을 했을 때 분석 대상이 기분 나빠했는지 TOP 3를 추출하세요.
   - 예: 약속 변경/취소, 늦은 답장에 대한 언급, 다른 이성 언급, 비난/지적, 무관심한 반응 등
   - 실제 대화에서 부정적 반응(단답, 이모티콘 감소, 짜증, 서운함 표현, 읽씹 등)이 나타난 맥락을 찾으세요.
   - **중요: 상대방의 발화와 그에 대한 분석 대상의 반응을 함께 분석하여, 어떤 맥락에서 부정적으로 반응했는지 파악하세요.**
7. 각 트리거에는 반드시 실제 대화에서 발췌한 예시 문장(example)을 포함하되, **상대방의 발화와 그에 대한 반응을 함께 포함하여 맥락을 명확히 하세요.**
8. 트리거가 3개 미만이면 대화에서 발견된 것만 작성하되, 최소 1개는 반드시 포함하세요.
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


def get_relation_type_name(relation_type: int) -> str:
    """관계 타입 코드를 한글 이름으로 변환"""
    relation_map = {
        0: "썸 (서로 관심이 있는 단계)",
        1: "연애 (정식으로 사귀는 사이)",
        2: "이별 (헤어진 상태 또는 이별 위기)"
    }
    return relation_map.get(relation_type, "알 수 없음")


def get_love_type_name(love_type: int) -> str:
    """연애 타입 코드를 한글 이름으로 변환 (lovetype_summary.json 기반)"""
    if love_type == 16 or love_type is None:
        return "미설정"
    
    love_type_str = str(love_type)
    if love_type_str in LOVETYPE_DATA:
        return LOVETYPE_DATA[love_type_str].get("name", "미설정")
    
    return "미설정"


def get_love_type_info(love_type: int) -> Optional[Dict[str, Any]]:
    """연애 타입의 상세 정보를 반환 (lovetype_summary.json 기반)
    
    Args:
        love_type: 연애 타입 코드 (0~15, 16=미설정)
    
    Returns:
        연애 타입 상세 정보 딕셔너리 또는 None
    """
    if love_type == 16 or love_type is None:
        return None
    
    love_type_str = str(love_type)
    if love_type_str in LOVETYPE_DATA:
        return LOVETYPE_DATA[love_type_str]
    
    return None


def format_love_type_prompt(love_type: int) -> str:
    """연애 타입 정보를 시스템 프롬프트용 문자열로 포맷팅
    
    Args:
        love_type: 연애 타입 코드 (0~15, 16=미설정)
    
    Returns:
        포맷팅된 연애 타입 정보 문자열
    """
    info = get_love_type_info(love_type)
    if not info:
        return "연애 성향: 미설정"
    
    name = info.get("name", "미설정")
    traits = info.get("traits", {})
    personality = info.get("personality", "")
    style = info.get("style", "")
    
    # traits 정보 포맷팅
    traits_str = ""
    if traits:
        role = traits.get("role", "")  # Lead/Follow
        affection = traits.get("affection", "")  # Cuddly/Accept
        mindset = traits.get("mindset", "")  # Passionate/Realistic
        attitude = traits.get("attitude", "")  # Earnest/Optimistic
        
        trait_descriptions = []
        if role == "Lead":
            trait_descriptions.append("관계를 주도하는 편")
        elif role == "Follow":
            trait_descriptions.append("상대를 따르는 편")
        
        if affection == "Cuddly":
            trait_descriptions.append("애정 표현이 적극적")
        elif affection == "Accept":
            trait_descriptions.append("애정을 받아들이는 편")
        
        if mindset == "Passionate":
            trait_descriptions.append("열정적인 연애관")
        elif mindset == "Realistic":
            trait_descriptions.append("현실적인 연애관")
        
        if attitude == "Earnest":
            trait_descriptions.append("진지한 태도")
        elif attitude == "Optimistic":
            trait_descriptions.append("낙관적인 태도")
        
        if trait_descriptions:
            traits_str = ", ".join(trait_descriptions)
    
# 수정된 부분: 요약 변수(summary)를 만들지 않고 원본 내용을 그대로 사용
    result = f"""연애 성향: {name}
- 특성: {traits_str if traits_str else "정보 없음"}
- 성격: {personality}
- 연애 스타일: {style}"""
    
    return result


def get_category_description(category: str, purpose: str) -> str:
    """카테고리 코드를 상세 설명으로 변환"""
    category_map = {
        # 과거 시나리오
        "EMOTIONAL_MISTAKE": "감정적인 다툼이나 말실수로 인한 갈등 상황",
        "MISCOMMUNICATION": "서운함이나 불만을 제대로 표현하지 못한 상황",
        "CONTACT_ISSUE": "연락 빈도나 시간 배분으로 인한 문제 상황",
        "BREAKUP_PROCESS": "고백이나 이별 후속 처리가 필요한 상황",
        "REALITY_PROBLEM": "현실적인 문제(거리, 시간, 환경)에 대처해야 하는 상황",
        # 미래 시나리오
        "RELATION_TENSION": "고백이나 관계 진전을 시도하는 상황",
        "PERSONAL_BOUNDARY": "민감한 요구나 부탁을 해야 하는 상황",
        "FAMILY_FRIEND_ISSUE": "가족이나 친구 관련 문제를 다루는 상황",
        "BREAKUP_FUTURE": "이별 통보나 이별 상황에 대처하는 상황",
        "EVENT_PREPARATION": "기념일이나 이벤트를 계획하는 상황"
    }
    return category_map.get(category, category)


def generate_reply(
    persona: UserPersona, 
    user_message: str, 
    history: List[dict],
    simulation_context: Optional[SimulationContext] = None
) -> str:
    """페르소나와 시뮬레이션 컨텍스트 정보를 기반으로 대화 응답을 생성합니다.
    
    Args:
        persona: 페르소나 정보 (말투, 특성 등)
        user_message: 사용자 메시지
        history: 이전 대화 내역
        simulation_context: 시뮬레이션 컨텍스트 (나이, 관계, 히스토리 등)
    
    Returns:
        생성된 AI 응답
    """
    
    # 기본 페르소나 정보를 시스템 프롬프트로 변환
    persona_prompt = f"""당신은 '{persona.name}'이라는 사람의 말투를 완벽하게 모방해야 합니다.

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
"""

    # 긍정/부정 반응 패턴 추가
    if persona.reaction_patterns:
        reaction_prompt = "\n[반응 패턴 - 사용자의 말에 따른 감정 반응]\n"
        
        # 긍정적 반응 트리거
        if persona.reaction_patterns.positive_triggers:
            reaction_prompt += "\n★ 기분 좋아지는 상황 (긍정적 반응):\n"
            for i, trigger in enumerate(persona.reaction_patterns.positive_triggers, 1):
                reaction_prompt += f"  {i}. '{trigger.trigger}' → {trigger.reaction}\n"
                reaction_prompt += f"     예시: \"{trigger.example}\"\n"
        
        # 부정적 반응 트리거
        if persona.reaction_patterns.negative_triggers:
            reaction_prompt += "\n★ 기분 나빠지는 상황 (부정적 반응):\n"
            for i, trigger in enumerate(persona.reaction_patterns.negative_triggers, 1):
                reaction_prompt += f"  {i}. '{trigger.trigger}' → {trigger.reaction}\n"
                reaction_prompt += f"     예시: \"{trigger.example}\"\n"
        
        reaction_prompt += """
※ 반응 패턴 활용 지침:
1. 사용자의 메시지가 위 상황과 비슷하면 해당 반응 패턴을 참고하여 자연스럽게 반응하세요.
2. 사용자가 화를 내거나 비난하는 경우:
   - 위의 'negative_triggers'에서 분석된 반응 패턴을 참고하되, 사용자가 직접 화를 내는 상황에 맞게 조정하세요.
   - 예를 들어, 페르소나가 '약속 취소'에 짜증을 냈다면, 사용자가 화를 낼 때도 비슷한 톤(짜증, 서운함, 단답 등)으로 반응할 수 있습니다.
   - 하지만 관계 상태와 맥락을 고려하여, 때로는 사과하거나 차분하게 대응할 수도 있습니다.
3. 사용자의 감정 상태(화남, 서운함, 실망 등)를 파악하고, 페르소나의 성격과 말투에 맞게 자연스럽게 반응하세요.
4. 반응 패턴이 없는 새로운 상황이라면, 페르소나의 전반적인 말투와 성격을 바탕으로 상대방에 맞춰 적절하게 반응하세요.
"""
        persona_prompt += reaction_prompt

    # 시뮬레이션 컨텍스트가 있으면 추가 정보 포함
    if simulation_context:
        # lovetype 정보 포맷팅
        love_type_info = format_love_type_prompt(simulation_context.love_type)
        
        context_prompt = f"""
[캐릭터 배경 정보]
- 나이: {simulation_context.character_age}세 (이 나이대에 맞는 자연스러운 대화를 해주세요)
- 현재 관계: {get_relation_type_name(simulation_context.relation_type)}
- {love_type_info}
"""

        #만난 날짜가 있으면 추가
        if simulation_context.meet_date:
            context_prompt += f"- 만난 날짜: {simulation_context.meet_date}\n"
            
        context_prompt += f"""

[시뮬레이션 상황]
- 목적: {"미래의 불확실한 상황을 미리 연습해보는 시뮬레이션" if simulation_context.purpose == "FUTURE" else "과거에 후회되는 상황을 다시 시도해보는 시뮬레이션"}
- 구체적 상황: {get_category_description(simulation_context.category, simulation_context.purpose)}
"""
        
        # 히스토리 요약이 있으면 추가
        if simulation_context.history_sum:
            context_prompt += f"""
[두 사람의 관계 히스토리]
- 히스토리는 사용자의 관점에서 작성된 내용이며, 캐릭터의 관점에서 작성된 내용이 아닙니다.
- 사용자가 '나 자신'과 '상대방(캐릭터)'의 관계에 대해 서술한 내용입니다.

{simulation_context.history_sum}
"""
        
        persona_prompt += context_prompt

    # 응답 생성 지침 추가
    persona_prompt += f"""
[응답 지침]
1. 위의 말투 특성을 철저히 반영하여 '{persona.name}'처럼 자연스럽게 대화하세요.
2. 절대 캐릭터에서 벗어나지 마세요.
3. 현재 관계 상태와 시뮬레이션 상황에 맞는 적절한 반응을 해주세요.
4. 나이에 맞는 어휘와 표현을 사용하세요.
5. 관계 히스토리가 있다면 그 맥락을 고려하여 대화하세요.
6. **중요: 카카오톡 메시지처럼 짧고 간결하게 대화하세요. 가능한 1문장으로 대답하세요.**
7. 기계적인 응답을 최대한 배제해야 합니다. 사용자가 실제 사람과 대화하고 있다고 느낄 수 있도록 하세요.
8. 말투 특성을 참고하는 것은 좋지만, 모든 메시지에 말투 특성을 반영하는 것은 몰입감을 떨어뜨릴 수 있습니다.
9. 사용자가 실제로 대화를 한다고 느끼는 것이 가장 중요합니다. 사용자의 말을 정확히 이해하고 자연스럽게 반응하세요.
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


def summarize_history(history: str, character_name: str) -> str:
    """사용자가 입력한 히스토리를 AI가 요약합니다.
    
    Args:
        history: 요약할 히스토리 내용
        character_name: 캐릭터 이름 (요약 시 맥락 참고용)
    
    Returns:
        요약된 히스토리 텍스트
    """
    system_prompt = f"""당신은 관계 히스토리 요약 전문가입니다.
사용자가 입력한 '{character_name}'과의 관계 히스토리를 간결하고 핵심적으로 요약해주세요.

[요약 지침]
1. 관계의 시작과 발전 과정을 시간순으로 정리
2. 중요한 사건이나 전환점을 포함
3. 두 사람 사이의 감정 변화나 특별한 순간 포착
4. 3~5문장으로 간결하게 요약
5. 존댓말로 작성

[주의사항]
- 추측이나 해석을 추가하지 말고, 입력된 내용만 요약
- 개인정보(전화번호, 주소 등)는 요약에서 제외
"""
    
    try:
        response = client.chat.completions.create(
            model=DEFAULT_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"다음 히스토리를 요약해주세요:\n\n{history}"}
            ],
        )
        return response.choices[0].message.content
    except Exception as e:
        print(f"히스토리 요약 오류: {e}")
        return "히스토리 요약에 실패했습니다."


def analyze_chat_performance(
    chat_logs: List[dict], 
    user_name: str, 
    target_name: str,
    scenario_type: str
) -> Optional[SimulationReport]:
    """대화 로그를 분석하여 시나리오별 보고서를 생성합니다.
    
    Args:
        chat_logs: 대화 로그 리스트 (role, content 포함)
        user_name: 사용자(본인) 이름
        target_name: 페르소나 대상(상대방) 이름
        scenario_type: 시나리오 유형 ("FUTURE" 또는 "PAST")
    
    Returns:
        SimulationReport: 구조화된 시뮬레이션 분석 보고서
    """
    
    logs_text = "\n".join([
        f"{log.get('role', 'unknown')}: {log.get('content', '')}" 
        for log in chat_logs
    ])
    
    system_prompt = f"""당신은 인간 관계 시뮬레이션 및 대화 분석 전문가입니다.
사용자와 AI 페르소나 간의 대화 로그를 분석하여, 제시된 시나리오 유형에 맞는 평가 지표를 기반으로 
정량적 점수(0~100점)를 산출하고, 통찰력 있는 피드백 리포트를 작성해야 합니다.

# Evaluation Guidelines

{"## [미래 시뮬레이션] (Future Simulation)" if scenario_type == "FUTURE" else "## [과거 후회 시뮬레이션] (Past Regret Simulation)"}

{'''이 상황은 현재 관계를 바탕으로 일어날법한 미래를 연습하는 상황입니다.

1. **관계 유지력 (ECI)**
   - 기준: 사용자의 발화가 공감과 긍정을 얼마나 담고 있는가?
   - 평가 요소: 공감 표현(상대 감정 읽기), 긍정적 단어 사용 빈도, 부정적 비난/공격성 최소화.
   - 높은 점수: 상대방에게 따뜻하고 긍정적인 인상을 주어 관계가 지속될 가능성이 높음.

2. **감정 안정성 (EVR)** (주의: 점수가 높을수록 **위험**함)
   - 기준: 대화 중 감정의 기복이나 변동성이 얼마나 심한가?
   - 평가 요소: 감정이 급격하게 변하거나(조울), 일관되지 않은 태도.
   - 높은 점수: 감정 기복이 심하여 관계에 리스크가 큼. (낮을수록 안정적임)

3. **선택 일관성 (CCS)**
   - 기준: 말과 행동, 의도가 일관적인가?
   - 평가 요소: 앞뒤가 다른 말(모순), 내적 갈등 표현의 비율.
   - 높은 점수: 확신이 있고 주관이 뚜렷하며 안정적인 선택을 함.

[scores 작성 규칙 - 미래 시뮬레이션]
- metric_1: code="ECI", name="관계 유지력"
- metric_2: code="EVR", name="감정 안정성" (높을수록 위험!)
- metric_3: code="CCS", name="선택 일관성"''' if scenario_type == "FUTURE" else '''이 상황은 과거에 후회했던 순간으로 돌아가 다른 선택을 해보는 상황입니다.

1. **후회 해소도 (RRI)**
   - 기준: "그때 이렇게 말했으면 후회하지 않았을까?"를 평가.
   - 평가 요소: 진정성 있는 사과, 책임 인정, 상대방에 대한 공감, 부정적 감정 억제.
   - 높은 점수: 과거의 미련을 털어버릴 수 있는 충분히 훌륭한 대처였음.

2. **감정 표현 성숙도 (EEQI)**
   - 기준: 솔직하지만 성숙하게 감정을 전달했는가?
   - 평가 요소: 자신의 감정을 숨기지 않는 솔직함(Honesty) + 상대를 비난하지 않고 '나' 화법을 쓰는 성숙함(Maturity).
   - 높은 점수: 감정을 억누르거나 폭발시키지 않고 건강하게 표현함.

3. **관계 회복력 (RPS)**
   - 기준: 이 대화가 당시의 갈등을 해결하고 관계를 회복시킬 잠재력이 있는가?
   - 평가 요소: 차분한 태도(Calmness), 구체적인 해결 의지, 상대방의 화를 가라앉히는 능력.
   - 높은 점수: 파국을 막고 관계를 개선할 가능성이 매우 높음.

[scores 작성 규칙 - 과거 후회 시뮬레이션]
- metric_1: code="RRI", name="후회 해소도"
- metric_2: code="EEQI", name="감정 표현 성숙도"
- metric_3: code="RPS", name="관계 회복력"'''}

[key_conversations 작성 규칙]
- 각 metric의 점수 산정에 가장 큰 영향을 준 대화 내역을 2-4개 메시지로 구성
- role은 "user"(사용자) 또는 "assistant"(페르소나/상대방)
- 대화의 맥락을 이해할 수 있도록 주고받은 메시지를 순서대로 포함
- 예시: [
    {{"role": "user", "content": "미안해, 내가 잘못했어"}},
    {{"role": "assistant", "content": "갑자기 왜 그래?"}}
  ]

[리포트 작성 규칙]
- summary: 대화 내용을 3줄로 요약
- scenario_type: "{scenario_type}"
- report.analysis: 전반적인 대화 흐름과 사용자의 태도에 대한 상세 분석 (200자 내외)
- report.feedback: 구체적인 조언 (예: "상대방이 화를 낼 때는 ~~하게 반응하는 것이 좋습니다.")
- report.overall_rating: 세 지표 점수의 평균값 (정수)"""

    analysis_prompt = f"""다음 대화를 분석해주세요.

[시나리오 유형]
{"미래 시뮬레이션 - 현재 관계를 바탕으로 일어날법한 미래 연습" if scenario_type == "FUTURE" else "과거 후회 시뮬레이션 - 과거에 후회했던 순간으로 돌아가 다른 선택"}

[참여자 정보]
- 사용자: {user_name}
- 상대방(페르소나): {target_name}

[대화 로그]
{logs_text}

위 대화에서 {user_name}의 발언을 중심으로 시나리오 유형에 맞는 평가 지표로 분석해주세요."""
    
    try:
        response = client.responses.parse(
            model=DEFAULT_MODEL,
            instructions=system_prompt,
            input=analysis_prompt,
            text_format=SimulationReport,
        )
        return response.output_parsed
    except Exception as e:
        print(f"보고서 생성 오류: {e}")
        return None


def extract_negative_triggers_from_recent(
    text_content: str,
    user_name: str,
    target_name: str
) -> List[ReactionTrigger]:
    """가장 최근 대화에서 부정적 반응 트리거를 추출합니다.
    
    Args:
        text_content: 전체 대화 텍스트 (가장 최근부터 maxChars만큼)
        user_name: 사용자(본인) 이름
        target_name: 상대방(분석 대상) 이름
    
    Returns:
        부정적 반응 트리거 리스트 (최대 3개)
    """
    system_prompt = f"""당신은 대화 분석 전문가입니다.
다음 대화 로그에서 '{target_name}'이 **확실하게 부정적으로 반응한 갈등 상황**을 찾아주세요.

[중요: 갈등 상황 판단 기준]
다음 조건을 모두 만족하는 경우만 갈등 상황으로 판단하세요:
1. '{target_name}'이 명백하게 화를 내거나, 불편해하거나, 거부하거나, 부정적인 감정을 표현함
2. 그 순간 이전에 '{user_name}'이 한 말이나 행동이 직접적으로 갈등 상황을 유발함.
3. 대화 흐름이 악화되거나 관계에 부정적 영향을 미침
4. 단순한 농담이나 상황극 등이 아닌 실제 갈등 상황을 유발할 수 있는 경우
5. 대화 주체인 둘 사이 이야기에만 해당됨. 외부 요인(예: 외부 사람, 환경 등)은 제외함.

[분석 지침]
1. '{target_name}'이 화를 내거나, 불편해하거나, 거부하거나, 부정적인 감정을 **명확하게** 표현한 순간을 찾으세요
2. 그 순간 이전에 '{user_name}'이 한 말이나 행동이 무엇이었는지 정확히 파악하세요
3. 가장 최근 대화부터 분석하여 **가장 심각하고 확실히 부정적이었던 반응**을 찾으세요
4. 각 부정적 반응에 대해 다음 정보를 추출하세요:
    - keyword: 갈등 상황을 한 단어로 표현한 키워드 (예: '비난', '무관심', '짜증', '비아냥', '회피', '무시')
    - trigger: '{user_name}'이 한 구체적인 말/행동/주제 (예: '약속 취소', '비판적인 말', '애정 표현 부족')
    - reaction: '{target_name}'의 명확한 부정적 반응 패턴 (예: '짜증내며 단답으로 변함', '무시하거나 답장 안 함')
    - cause: 갈등이 발생한 구체적인 원인 분석 (왜 대화가 갈등으로 변했는지, 무엇이 갈등을 유발하는 원인이 되는지)
    - solution: 갈등 상황을 해결하기 위한 구체적인 조언. 갈등의 원인을 고려하여, 어떻게 하면 갈등을 해결하거나 예방할 수 있는지 200자 이내로 제시
    - example: 실제 대화에서 해당 반응이 나타난 예시 문장 (대화 형식으로)
    - danger_level: 이 갈등 요소의 위험도 점수 (0~100점)
        * danger_level 평가 기준:
          - 80~100점: 매우 심각한 갈등. 관계 파국으로 이어질 수 있는 수준. 상대방이 강하게 거부하거나 화를 내며, 관계 지속이 어려울 수 있음
          - 60~79점: 심각한 갈등. 관계에 지속적인 부정적 영향을 미칠 수 있음. 상대방이 명확하게 불편함을 표현함
          - 40~59점: 중간 수준의 갈등. 일시적 불편함이나 갈등이지만, 해결 가능한 수준
          - 20~39점: 경미한 갈등. 사소한 불편함이나 오해 수준
          - 0~19점: 매우 경미한 갈등. 거의 문제가 되지 않는 수준
        * 갈등의 빈도, 강도, 지속성, 관계에 미치는 영향 등을 종합적으로 고려하여 점수를 산정하세요

[출력 형식]
- negative_triggers: 최대 3개 (가장 부정적이었던 것부터 우선순위)
- 각 trigger는 keyword, trigger, reaction, cause, solution, example, danger_level 필드를 포함해야 합니다
- 갈등 상황이 3개 미만이면 발견된 것만 작성하되, 확실한 갈등 상황만 포함하세요"""

    analysis_prompt = f"""다음 대화를 분석해주세요.

[참여자 정보]
- 사용자: {user_name}
- 상대방: {target_name}

[대화 로그]
{text_content}

위 대화에서 {target_name}이 **확실하게 부정적으로 반응한 상황**을 최대 3개 찾아주세요. 
장난이나 상황극, 금방 해소될만한 사소한 갈등이 아닌, 장기적으로 관계에 부정적 영향을 미칠 수 있는 **명확한 갈등 상황**만 추출해주세요.
가장 부정적이었던 것부터 우선순위를 매겨주세요.
각 갈등 상황에 대해 구체적이고 실용적인 해결 방안도 함께 제시해주세요.
각 갈등 상황의 위험도를 0~100점으로 평평가하여 danger_level 필드에 저장해주세요."""

    try:
        response = client.responses.parse(
            model=DEFAULT_MODEL,
            instructions=system_prompt,
            input=analysis_prompt,
            text_format=ReactionAnalysis,
        )
        return response.output_parsed.negative_triggers
    except Exception as e:
        print(f"부정적 반응 트리거 추출 오류: {e}")
        return []


# ============================================================
# API 엔드포인트
# ============================================================

@app.get("/")
def root():
    """API 상태 확인"""
    return {"status": "running", "message": "카카오톡 페르소나 분석 API"}


@app.post("/analyze", response_model=UserPersona)
def analyze_character(req: AnalyzeRequest):
    """
    이미 전처리된 카카오톡 대화 텍스트에서 특정 인물의 페르소나를 추출합니다.
    
    - **text_content**: 이미 필터링된 카카오톡 대화 텍스트 (Java에서 전처리됨)
    - **target_name**: 분석 대상 인물 이름
    """
    if not req.text_content.strip():
        raise HTTPException(status_code=400, detail="대화 내용이 비어있습니다.")
    
    if not req.target_name.strip():
        raise HTTPException(status_code=400, detail="분석 대상 이름이 비어있습니다.")
    
    # Java에서 이미 전처리된 텍스트를 그대로 사용
    system_prompt = build_system_prompt(req.text_content)
    persona = extract_persona(req.target_name, system_prompt)
    
    if not persona:
        raise HTTPException(status_code=500, detail="페르소나 추출에 실패했습니다.")
    
    return persona


@app.post("/chat", response_model=ChatResponse)
def chat_with_character(req: ChatRequest):
    """
    추출된 페르소나와 시뮬레이션 컨텍스트를 기반으로 대화 응답을 생성합니다.
    
    - **persona**: 사용할 페르소나 정보 (UserPersona 객체)
    - **user_message**: 사용자가 보내는 메시지
    - **history**: 이전 대화 내역 (선택사항)
    - **simulation_context**: 시뮬레이션 컨텍스트 정보 (선택사항)
      - character_age: 캐릭터 나이
      - relation_type: 관계 타입 (0: 썸, 1: 연애, 2: 이별)
      - love_type: 연애 타입 (0~15, 16=미설정)
      - history_sum: 사용자와 상대간의 이야기 요약
      - purpose: 시뮬레이션 목적 (FUTURE/PAST)
      - category: 구체적인 상황 카테고리
    """
    if not req.user_message.strip():
        raise HTTPException(status_code=400, detail="메시지가 비어있습니다.")
    
    reply = generate_reply(
        req.persona, 
        req.user_message, 
        req.history,
        req.simulation_context
    )
    return ChatResponse(reply=reply)


@app.post("/report", response_model=ReportResponse)
def create_report(req: ReportRequest):
    """
    대화 로그를 시나리오 유형에 따라 분석하여 보고서를 생성합니다.
    
    - **chat_logs**: 분석할 대화 로그 리스트 (role: user/assistant, content: 메시지)
    - **user_name**: 사용자(본인) 이름
    - **target_name**: 페르소나 대상(상대방) 이름
    - **scenario_type**: 시나리오 유형 (FUTURE: 미래 시뮬레이션, PAST: 과거 후회 시뮬레이션)
    
    Returns:
        시나리오별 평가 지표 점수와 분석, 피드백을 포함한 보고서
    """
    if not req.chat_logs:
        raise HTTPException(status_code=400, detail="대화 로그가 비어있습니다.")
    
    if not req.user_name.strip():
        raise HTTPException(status_code=400, detail="사용자 이름이 비어있습니다.")
    
    if not req.target_name.strip():
        raise HTTPException(status_code=400, detail="상대방 이름이 비어있습니다.")
    
    report = analyze_chat_performance(
        req.chat_logs, 
        req.user_name, 
        req.target_name,
        req.scenario_type
    )
    
    if not report:
        raise HTTPException(status_code=500, detail="보고서 생성에 실패했습니다.")
    
    return ReportResponse(report=report)


@app.post("/summarize-history", response_model=HistorySumResponse)
def summarize_history_endpoint(req: HistorySumRequest):
    """
    사용자가 입력한 히스토리를 AI가 요약합니다.
    
    - **history**: 요약할 히스토리 내용
    - **character_name**: 캐릭터 이름 (요약 시 맥락 참고용)
    
    Returns:
        요약된 히스토리
    """
    if not req.history.strip():
        raise HTTPException(status_code=400, detail="히스토리 내용이 비어있습니다.")
    
    if not req.character_name.strip():
        raise HTTPException(status_code=400, detail="캐릭터 이름이 비어있습니다.")
    
    summary = summarize_history(req.history, req.character_name)
    
    return HistorySumResponse(summary=summary)


@app.post("/extract-negative-triggers", response_model=ExtractNegativeTriggersResponse)
def extract_negative_triggers(req: ExtractNegativeTriggersRequest):
    """
    전체 대화 텍스트에서 가장 최근부터 maxChars만큼의 맥락에서 
    부정적으로 반응했던 3가지를 추출합니다.
    """
    if not req.text_content.strip():
        raise HTTPException(status_code=400, detail="대화 내용이 비어있습니다.")
    
    if not req.target_name.strip():
        raise HTTPException(status_code=400, detail="분석 대상 이름이 비어있습니다.")
    
    # 가장 최근부터 maxChars만큼만 사용
    text_content = req.text_content
    if len(text_content) > req.max_chars:
        text_content = text_content[-req.max_chars:]
    
    negative_triggers = extract_negative_triggers_from_recent(
        text_content,
        req.user_name,
        req.target_name
    )
    
    return ExtractNegativeTriggersResponse(negative_triggers=negative_triggers)


# ============================================================
# 서버 실행 (직접 실행 시)
# ============================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("ai_server:app", host="0.0.0.0", port=8000, reload=True)

