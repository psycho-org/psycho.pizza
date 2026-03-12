-- Account 30건 시드 데이터
-- 비밀번호 원문: Password1234%^
-- password_hash: BCrypt 해시 고정값 사용

insert into public.accounts (
    id,
    created_at,
    updated_at,
    deleted_at,
    deleted_by,
    email,
    family_name,
    given_name,
    password_hash
)
select
    v.id::uuid,
    now(),
    now(),
    null,
    null,
    v.email,
    v.family_name,
    v.given_name,
    '$2y$10$C1A30gBA/3FGnDP1alHxyOUqOBjXfkoWy/nf.NtsjkWYw3g0QaSuq'
from (
    values
        -- A01: Owner
        ('00000000-0000-0000-0000-000000000001', 'seed+a01@psycho.local', '김', '도윤'),

        -- A02 ~ A05: 기획
        ('00000000-0000-0000-0000-000000000002', 'seed+a02@psycho.local', '이', '서준'),
        ('00000000-0000-0000-0000-000000000003', 'seed+a03@psycho.local', '박', '지안'),
        ('00000000-0000-0000-0000-000000000004', 'seed+a04@psycho.local', '최', '유진'),
        ('00000000-0000-0000-0000-000000000005', 'seed+a05@psycho.local', '정', '하람'),

        -- A06 ~ A09: 설계(UX/UI)
        ('00000000-0000-0000-0000-000000000006', 'seed+a06@psycho.local', '한', '예린'),
        ('00000000-0000-0000-0000-000000000007', 'seed+a07@psycho.local', '윤', '시우'),
        ('00000000-0000-0000-0000-000000000008', 'seed+a08@psycho.local', '장', '민서'),
        ('00000000-0000-0000-0000-000000000009', 'seed+a09@psycho.local', '조', '다온'),

        -- A10 ~ A14: 백엔드 개발
        ('00000000-0000-0000-0000-000000000010', 'seed+a10@psycho.local', '임', '현우'),
        ('00000000-0000-0000-0000-000000000011', 'seed+a11@psycho.local', '신', '도현'),
        ('00000000-0000-0000-0000-000000000012', 'seed+a12@psycho.local', '오', '준혁'),
        ('00000000-0000-0000-0000-000000000013', 'seed+a13@psycho.local', '서', '민재'),
        ('00000000-0000-0000-0000-000000000014', 'seed+a14@psycho.local', '권', '지후'),

        -- A15 ~ A19: 프론트 개발
        ('00000000-0000-0000-0000-000000000015', 'seed+a15@psycho.local', '황', '수빈'),
        ('00000000-0000-0000-0000-000000000016', 'seed+a16@psycho.local', '안', '나윤'),
        ('00000000-0000-0000-0000-000000000017', 'seed+a17@psycho.local', '송', '지민'),
        ('00000000-0000-0000-0000-000000000018', 'seed+a18@psycho.local', '류', '가온'),
        ('00000000-0000-0000-0000-000000000019', 'seed+a19@psycho.local', '전', '시안'),

        -- A20 ~ A23: DevOps/SRE
        ('00000000-0000-0000-0000-000000000020', 'seed+a20@psycho.local', '고', '태윤'),
        ('00000000-0000-0000-0000-000000000021', 'seed+a21@psycho.local', '문', '은호'),
        ('00000000-0000-0000-0000-000000000022', 'seed+a22@psycho.local', '양', '주원'),
        ('00000000-0000-0000-0000-000000000023', 'seed+a23@psycho.local', '손', '하준'),

        -- A24 ~ A27: QA
        ('00000000-0000-0000-0000-000000000024', 'seed+a24@psycho.local', '배', '채원'),
        ('00000000-0000-0000-0000-000000000025', 'seed+a25@psycho.local', '백', '유나'),
        ('00000000-0000-0000-0000-000000000026', 'seed+a26@psycho.local', '허', '서아'),
        ('00000000-0000-0000-0000-000000000027', 'seed+a27@psycho.local', '남', '예준'),

        -- A28 ~ A30: 사업운영(비IT)
        ('00000000-0000-0000-0000-000000000028', 'seed+a28@psycho.local', '노', '현정'),
        ('00000000-0000-0000-0000-000000000029', 'seed+a29@psycho.local', '구', '민아'),
        ('00000000-0000-0000-0000-000000000030', 'seed+a30@psycho.local', '유', '재희')
) as v(id, email, family_name, given_name)
where not exists (
    select 1
    from public.accounts a
    where lower(a.email) = lower(v.email)
      and a.deleted_at is null
);
