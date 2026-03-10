-- flyway:placeholderReplacement=false
-- WORKSPACE_INVITE 템플릿 시드 데이터

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
    'WORKSPACE_INVITE',
    '${workspaceName} 워크스페이스 초대',
    '워크스페이스 초대 메일',
    'WORKSPACE_INVITE_ACCEPT',
    true,
    24,
    $$<!doctype html>
<html>
  <body style="font-family: Arial, sans-serif; background:#f6f7fb; padding:24px;">
    <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:24px;text-align:center;">
      <h2 style="margin:0 0 12px 0;">${workspaceName} 워크스페이스 초대</h2>
      <p style="margin:0 0 12px 0;">
        ${inviterName} 님이 워크스페이스에 초대했습니다.
      </p>
      <p style="margin:0 0 20px 0;">
        아래 버튼을 누르면 인증이 완료됩니다.
      </p>
      <a href="${inviteLink}"
         style="display:inline-block;background:#1f6feb;color:#ffffff;text-decoration:none;
                padding:12px 18px;border-radius:8px;font-weight:bold;">
        초대 수락하기
      </a>
      <p style="margin:20px 0 0 0;font-size:12px;color:#6b7280;word-break:break-all;">
        버튼이 동작하지 않으면 아래 링크를 복사해 주세요.<br/>
        ${inviteLink}
      </p>
    </div>
  </body>
</html>$$
where not exists (
    select 1
    from mail_templates
    where
        mail_type = 'WORKSPACE_INVITE'
        and deleted_at is null
);

-- OTP 템플릿 시드 데이터
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
    'OTP',
    '[psycho] 인증번호 안내',
    'OTP 인증 메일',
    null,
    false,
    null,
    $$<!doctype html>
<html>
  <body style="font-family: Arial, sans-serif; background:#f6f7fb; padding:24px;">
    <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:24px;text-align:center;">
      <h2 style="margin:0 0 12px 0;">인증번호 안내</h2>
      <p style="margin:0 0 12px 0;">
        아래 OTP 번호를 입력해 인증을 완료해 주세요.
      </p>
      <p style="margin:0 0 12px 0;font-size:28px;font-weight:700;letter-spacing:4px;color:#1f6feb;">
        ${otpCode}
      </p>
      <p style="margin:0 0 12px 0;">
        유효 시간: ${expiresInMinutes}분
      </p>
      <p style="margin:0;font-size:12px;color:#6b7280;">
        사용 목적: ${otpPurpose}
      </p>
    </div>
  </body>
</html>$$
where not exists (
    select 1
    from mail_templates
    where
        mail_type = 'OTP'
        and deleted_at is null
);
