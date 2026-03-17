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
    v.joined_at,
    v.joined_at + interval '5 minutes',
    null,
    null,
    v.email,
    v.family_name,
    v.given_name,
    '$2y$10$C1A30gBA/3FGnDP1alHxyOUqOBjXfkoWy/nf.NtsjkWYw3g0QaSuq'
from (
    values
        -- A01: Owner
        ('00000000-0000-0000-0000-000000000001', 'seed+a01@psycho.local', '김', '도윤', '2025-12-09 09:00:00'::timestamp),

        -- A02 ~ A05: 기획
        ('00000000-0000-0000-0000-000000000002', 'seed+a02@psycho.local', '이', '서준', '2025-12-10 09:20:00'::timestamp),
        ('00000000-0000-0000-0000-000000000003', 'seed+a03@psycho.local', '박', '지안', '2025-12-11 10:10:00'::timestamp),
        ('00000000-0000-0000-0000-000000000004', 'seed+a04@psycho.local', '최', '유진', '2025-12-12 11:40:00'::timestamp),
        ('00000000-0000-0000-0000-000000000005', 'seed+a05@psycho.local', '정', '하람', '2025-12-15 09:35:00'::timestamp),

        -- A06 ~ A09: 설계(UX/UI)
        ('00000000-0000-0000-0000-000000000006', 'seed+a06@psycho.local', '한', '예린', '2025-12-16 14:15:00'::timestamp),
        ('00000000-0000-0000-0000-000000000007', 'seed+a07@psycho.local', '윤', '시우', '2025-12-17 09:50:00'::timestamp),
        ('00000000-0000-0000-0000-000000000008', 'seed+a08@psycho.local', '장', '민서', '2025-12-18 13:05:00'::timestamp),
        ('00000000-0000-0000-0000-000000000009', 'seed+a09@psycho.local', '조', '다온', '2025-12-19 10:45:00'::timestamp),

        -- A10 ~ A14: 백엔드 개발
        ('00000000-0000-0000-0000-000000000010', 'seed+a10@psycho.local', '임', '현우', '2025-12-22 09:00:00'::timestamp),
        ('00000000-0000-0000-0000-000000000011', 'seed+a11@psycho.local', '신', '도현', '2025-12-22 15:30:00'::timestamp),
        ('00000000-0000-0000-0000-000000000012', 'seed+a12@psycho.local', '오', '준혁', '2025-12-23 09:25:00'::timestamp),
        ('00000000-0000-0000-0000-000000000013', 'seed+a13@psycho.local', '서', '민재', '2025-12-24 11:10:00'::timestamp),
        ('00000000-0000-0000-0000-000000000014', 'seed+a14@psycho.local', '권', '지후', '2025-12-26 10:05:00'::timestamp),

        -- A15 ~ A19: 프론트 개발
        ('00000000-0000-0000-0000-000000000015', 'seed+a15@psycho.local', '황', '수빈', '2025-12-15 14:10:00'::timestamp),
        ('00000000-0000-0000-0000-000000000016', 'seed+a16@psycho.local', '안', '나윤', '2025-12-16 09:40:00'::timestamp),
        ('00000000-0000-0000-0000-000000000017', 'seed+a17@psycho.local', '송', '지민', '2025-12-17 13:20:00'::timestamp),
        ('00000000-0000-0000-0000-000000000018', 'seed+a18@psycho.local', '류', '가온', '2025-12-18 15:15:00'::timestamp),
        ('00000000-0000-0000-0000-000000000019', 'seed+a19@psycho.local', '전', '시안', '2025-12-19 16:00:00'::timestamp),

        -- A20 ~ A23: DevOps/SRE
        ('00000000-0000-0000-0000-000000000020', 'seed+a20@psycho.local', '고', '태윤', '2025-12-22 13:30:00'::timestamp),
        ('00000000-0000-0000-0000-000000000021', 'seed+a21@psycho.local', '문', '은호', '2025-12-23 14:20:00'::timestamp),
        ('00000000-0000-0000-0000-000000000022', 'seed+a22@psycho.local', '양', '주원', '2025-12-24 10:15:00'::timestamp),
        ('00000000-0000-0000-0000-000000000023', 'seed+a23@psycho.local', '손', '하준', '2025-12-24 16:25:00'::timestamp),

        -- A24 ~ A27: QA
        ('00000000-0000-0000-0000-000000000024', 'seed+a24@psycho.local', '배', '채원', '2025-12-25 09:05:00'::timestamp),
        ('00000000-0000-0000-0000-000000000025', 'seed+a25@psycho.local', '백', '유나', '2025-12-25 16:10:00'::timestamp),
        ('00000000-0000-0000-0000-000000000026', 'seed+a26@psycho.local', '허', '서아', '2025-12-26 11:30:00'::timestamp),
        ('00000000-0000-0000-0000-000000000027', 'seed+a27@psycho.local', '남', '예준', '2025-12-26 16:45:00'::timestamp),

        -- A28 ~ A30: 사업운영(비IT)
        ('00000000-0000-0000-0000-000000000028', 'seed+a28@psycho.local', '노', '현정', '2025-12-27 09:35:00'::timestamp),
        ('00000000-0000-0000-0000-000000000029', 'seed+a29@psycho.local', '구', '민아', '2025-12-27 13:05:00'::timestamp),
        ('00000000-0000-0000-0000-000000000030', 'seed+a30@psycho.local', '유', '재희', '2025-12-27 17:55:00'::timestamp)
) as v(id, email, family_name, given_name, joined_at)
where not exists (
    select 1
    from public.accounts a
    where lower(a.email) = lower(v.email)
      and a.deleted_at is null
);
