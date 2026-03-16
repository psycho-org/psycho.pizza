-- flyway:placeholderReplacement=false
-- EMAIL_ALREADY_EXISTS 템플릿 시드 데이터

insert into mail_templates
(
    id,
    created_at,
    updated_at,
    deleted_at,
    deleted_by,
    mail_type,
    title,
    description,
    action_type,
    token_auth_enabled,
    token_expire_hours,
    html_content
)
select
    gen_random_uuid(),
    now(),
    now(),
    null,
    null,
    'EMAIL_ALREADY_EXISTS',
    '[psycho] 이미 가입된 이메일 안내',
    '이미 가입된 이메일 안내 메일',
    null,
    false,
    null,
    $$<!doctype html>
<html>
  <body style="font-family: Arial, sans-serif; background:#f6f7fb; padding:24px;">
    <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:24px;text-align:center;">
      <h2 style="margin:0 0 12px 0;">이미 가입된 이메일입니다</h2>
      <p style="margin:0 0 12px 0;">
        ${name} 님, <strong>${email}</strong> 계정은 이미 가입되어 있습니다.
      </p>
      <p style="margin:0 0 12px 0;">
        가입일자: ${joinedAt}
      </p>
      <p style="margin:0 0 20px 0;">
        아래 버튼을 누르면 로그인 페이지로 이동할 수 있습니다.
      </p>
      <div>
        <a href="${url}"
           style="display:inline-block;background:#111827;color:#ffffff;text-decoration:none;
                  padding:12px 18px;border-radius:8px;font-weight:bold;">
          로그인하러 가기
        </a>
      </div>
      <p style="margin:20px 0 0 0;font-size:12px;color:#6b7280;word-break:break-all;">
        버튼이 동작하지 않으면 아래 링크를 복사해 주세요.<br/>
        ${url}
      </p>
    </div>
  </body>
</html>$$
where not exists (
    select 1
    from mail_templates
    where
        mail_type = 'EMAIL_ALREADY_EXISTS'
        and deleted_at is null
);

