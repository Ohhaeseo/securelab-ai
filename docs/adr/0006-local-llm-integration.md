# 0006. 로컬 LLM 연동: Spring AI 대신 Ollama REST 직접 호출

- 상태: 채택
- 날짜: 2026-11-21

## 배경

기획서 기술 스택은 "Ollama + Spring AI"다. 6주차에 실제 소형 모델(1~3B)로 AI-01 벤치마크를 해야 한다. 이 PC에는 CUDA GPU가 없다(Intel Arc 140V 내장 GPU) — CPU로 실행한다.

## 결정

- Ollama `/api/chat`을 Spring `RestClient`로 **직접** 호출한다 (`OllamaChatGateway`). `format: "json"`으로 `{"answer", "tool_calls"}` 출력을 요청하고 `ModelOutputParser`가 해석한다.
- Ollama 주소는 **루프백만 허용**한다(`securelab.ai.ollama.allow-remote=false` 기본). 문서가 외부 모델 서버로 나가지 않는다.
- 시스템 지시에 서버가 허용한 도구 목록을 넣는다. 문서 본문은 `&lt;` 등으로 이스케이프해 `<document>` 데이터 영역을 닫고 나오지 못하게 한다.
- 모델: `qwen2.5:1.5b`(986MB), `gemma3:1b`(806MB). 한국어 지원, CPU에서 응답 1~5초.

## 검토한 대안

- **Spring AI ChatClient:** 기획서 원안. 하지만 도구 호출을 프레임워크가 자동 실행하는 경로를 막아야 하고(ADR-0005: 판정 지점은 `ToolExecutionPolicyService` 하나), 소형 모델의 깨진 출력을 어떻게 다루는지 직접 보여 주는 편이 실험 목적에 맞다. Boot 4.1용 Spring AI 2.x API를 새로 익히는 비용도 있다. 모델 수가 늘거나 스트리밍·임베딩이 필요해지면 다시 검토한다.
- **OpenAI 호환 엔드포인트(`/v1/chat/completions`):** 이식성은 좋지만 Ollama 고유 옵션(시드, JSON 형식) 설정이 덜 직접적이다.

## 결과

- 소형 모델은 JSON 모드에서 같은 토큰을 반복하다 Ollama가 생성을 중단하는 경우가 있다(`prediction aborted, token repeat limit reached`, gemma3:1b에서 관찰). 벤치마크는 시드를 바꿔 최대 3번 시도하고, 그래도 실패하면 "모델 오류"로 따로 센다.
- 두 모델을 번갈아 쓸 때 Ollama 데스크톱 앱 서버에서 실행기 오류가 있었다. 실험할 때는 `OLLAMA_MAX_LOADED_MODELS=1`로 띄운 `ollama serve`를 권장한다.
- 도우미 기본 모델은 여전히 Mock이다. `securelab.ai.provider=ollama`로 바꾸면 `securelab.ai.ollama.model`을 쓴다.
