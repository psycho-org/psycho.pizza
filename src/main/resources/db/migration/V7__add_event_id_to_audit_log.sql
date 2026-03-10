-- 1. 먼저 NULL 허용 상태로 event_id 컬럼 추가
ALTER TABLE audit_log ADD COLUMN event_id UUID;

-- 2. 기존에 존재하던 로그 데이터가 있다면, 각각 고유한 UUID로 채워주기 (PostgreSQL 13+ 내장 함수 사용)
UPDATE audit_log SET event_id = gen_random_uuid() WHERE event_id IS NULL;

-- 3. 모든 데이터가 채워진 후 NOT NULL 제약조건 설정
ALTER TABLE audit_log ALTER COLUMN event_id SET NOT NULL;