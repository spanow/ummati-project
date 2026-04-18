## Script de test E2E — Ummati Backend API (v3 - Complet)
## Exécuter dans PowerShell. Le backend doit tourner sur localhost:8080.
## NOTE: Le rate-limit est de 20 req/min sur /auth/**, des pauses sont insérées.

$base = "http://localhost:8080/api/v1"
$ts = Get-Date -Format "yyyyMMddHHmmss"
$email = "testuser_$ts@test.com"
$email2 = "testuser2_$ts@test.com"
$password = "Test123!@#"
$pass_count = 0
$fail_count = 0
$skip_count = 0
$auth_call_count = 0

function Wait-RateLimit {
    $script:auth_call_count++
    if ($script:auth_call_count % 18 -eq 0) {
        Write-Host "    [PAUSE] Attente 62s pour reset du rate limit..." -ForegroundColor DarkCyan
        Start-Sleep -Seconds 62
    }
}

function Test-Api($name, $method, $url, $body, $headers, $expectedStatus) {
    # Track auth calls for rate limiting
    if ($url -match "/auth/") { Wait-RateLimit }

    try {
        $params = @{
            Uri = $url; Method = $method; ContentType = "application/json"
            UseBasicParsing = $true; TimeoutSec = 10
        }
        if ($body) { $params.Body = ($body | ConvertTo-Json -Depth 5) }
        if ($headers) { $params.Headers = $headers }
        $r = Invoke-WebRequest @params -ErrorAction Stop
        $status = [int]$r.StatusCode
        try { $content = $r.Content | ConvertFrom-Json } catch { $content = $null }
    } catch {
        $status = [int]$_.Exception.Response.StatusCode.value__
        if (-not $status) { $status = 0 }
        try {
            $sr = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($sr)
            $body_text = $reader.ReadToEnd()
            $content = $body_text | ConvertFrom-Json
        } catch { $content = $null }
    }
    $pass = if ($expectedStatus) { $status -eq $expectedStatus } else { $status -ge 200 -and $status -lt 300 }
    if ($pass) {
        Write-Host "  [PASS] $name -> HTTP $status" -ForegroundColor Green
        $script:pass_count++
    } else {
        Write-Host "  [FAIL] $name -> HTTP $status (expected $expectedStatus)" -ForegroundColor Red
        if ($content) { Write-Host "         $($content | ConvertTo-Json -Compress -Depth 3)" -ForegroundColor DarkYellow }
        $script:fail_count++
    }
    return @{ name=$name; status=$status; pass=$pass; data=$content }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  TESTS E2E COMPLETS - Ummati API" -ForegroundColor Cyan
Write-Host "  User1: $email" -ForegroundColor Cyan
Write-Host "  User2: $email2" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# ================================================================
#  0. HEALTH CHECK
# ================================================================
Write-Host "`n--- 0. HEALTH CHECK ---"
$h = Test-Api "Health check" "GET" "http://localhost:8080/actuator/health" $null $null 200
if (-not $h.pass) { Write-Host "Backend non disponible. Arret." -ForegroundColor Red; exit 1 }

# ================================================================
#  1. INSCRIPTION — User 1
# ================================================================
Write-Host "`n--- 1. INSCRIPTION (User 1) ---"
Test-Api "Register user1" "POST" "$base/auth/register" @{email=$email; password=$password; firstName="Test"; lastName="User"} $null 201

# ================================================================
#  1b. INSCRIPTION — User 2 (pour tests multi-user)
# ================================================================
Write-Host "`n--- 1b. INSCRIPTION (User 2) ---"
Test-Api "Register user2" "POST" "$base/auth/register" @{email=$email2; password=$password; firstName="Alice"; lastName="Volunteer"} $null 201

# ================================================================
#  1c. VALIDATION — Inscription avec données invalides
# ================================================================
Write-Host "`n--- 1c. VALIDATION INSCRIPTION ---"
Test-Api "Register email invalide" "POST" "$base/auth/register" @{email="invalid"; password=$password; firstName="X"; lastName="Y"} $null 400
Test-Api "Register mdp trop court" "POST" "$base/auth/register" @{email="short@test.com"; password="abc"; firstName="X"; lastName="Y"} $null 400
Test-Api "Register champs manquants" "POST" "$base/auth/register" @{email=""; password=""; firstName=""; lastName=""} $null 400
Test-Api "Register email deja pris" "POST" "$base/auth/register" @{email=$email; password=$password; firstName="Dup"; lastName="User"} $null 409

# ================================================================
#  2. LOGIN AVANT VERIF EMAIL (devrait echouer)
# ================================================================
Write-Host "`n--- 2. LOGIN (avant verif email, 403 attendu) ---"
Test-Api "Login avant verif" "POST" "$base/auth/login" @{email=$email; password=$password} $null 403

# ================================================================
#  2b. LOGIN — Cas d'erreur
# ================================================================
Write-Host "`n--- 2b. LOGIN erreurs ---"
Test-Api "Login email inexistant" "POST" "$base/auth/login" @{email="nobody@nowhere.com"; password=$password} $null 400
Test-Api "Login mdp incorrect" "POST" "$base/auth/login" @{email=$email; password="WrongPassword1!@"} $null 403

# ================================================================
#  3. CONFIRM EMAIL VIA TOKEN DB — User 1
# ================================================================
Write-Host "`n--- 3. CONFIRM EMAIL (User 1 via DB) ---"
$sql = "SELECT token FROM verification_tokens WHERE user_id = (SELECT id FROM users WHERE email='" + $email + "') ORDER BY created_at DESC LIMIT 1;"
$tokenRaw = docker exec ummati-postgres psql -U ummati -d ummati_db -t -A -c $sql 2>$null
$token = if ($tokenRaw) { ($tokenRaw | Out-String).Trim() } else { "" }
if ($token -and $token.Length -gt 5) {
    Test-Api "Confirm email user1 (POST)" "POST" "$base/auth/confirm-email?token=$token" $null $null 200
} else {
    Write-Host "  [SKIP] Token non trouve en DB" -ForegroundColor Yellow
    $skip_count++
}

# ================================================================
#  3b. CONFIRM EMAIL — Token invalide
# ================================================================
Write-Host "`n--- 3b. CONFIRM EMAIL (token invalide) ---"
Test-Api "Confirm email token bidon" "POST" "$base/auth/confirm-email?token=fake-token-12345" $null $null 400

# ================================================================
#  3c. CONFIRM EMAIL — User 2
# ================================================================
Write-Host "`n--- 3c. CONFIRM EMAIL (User 2 via DB) ---"
$sql2 = "SELECT token FROM verification_tokens WHERE user_id = (SELECT id FROM users WHERE email='" + $email2 + "') ORDER BY created_at DESC LIMIT 1;"
$tokenRaw2 = docker exec ummati-postgres psql -U ummati -d ummati_db -t -A -c $sql2 2>$null
$token2 = if ($tokenRaw2) { ($tokenRaw2 | Out-String).Trim() } else { "" }
if ($token2 -and $token2.Length -gt 5) {
    Test-Api "Confirm email user2 (POST)" "POST" "$base/auth/confirm-email?token=$token2" $null $null 200
} else {
    Write-Host "  [SKIP] Token user2 non trouve en DB" -ForegroundColor Yellow
    $skip_count++
}

# ================================================================
#  4. LOGIN APRES VERIF — User 1
# ================================================================
Write-Host "`n--- 4. LOGIN (User 1 apres verif email) ---"
$login = Test-Api "Login user1" "POST" "$base/auth/login" @{email=$email; password=$password} $null 200
$accessToken = $login.data.data.accessToken
$refreshToken = $login.data.data.refreshToken
$authH = @{ Authorization = "Bearer $accessToken" }
if ($accessToken) {
    Write-Host "    Token: $($accessToken.Substring(0, [Math]::Min(30, $accessToken.Length)))..." -ForegroundColor DarkGray
}

# ================================================================
#  4b. LOGIN — User 2
# ================================================================
Write-Host "`n--- 4b. LOGIN (User 2) ---"
$login2 = Test-Api "Login user2" "POST" "$base/auth/login" @{email=$email2; password=$password} $null 200
$accessToken2 = $login2.data.data.accessToken
$authH2 = @{ Authorization = "Bearer $accessToken2" }

# ================================================================
#  5. PROFIL — Accès sans token (403 — Spring Security default)
# ================================================================
Write-Host "`n--- 5. PROFIL (sans token = 403) ---"
Test-Api "Get profile sans token" "GET" "$base/profile" $null $null 403

# ================================================================
#  5b. PROFIL — Accès avec token
# ================================================================
Write-Host "`n--- 5b. PROFIL (avec token) ---"
$prof = Test-Api "Get profile user1" "GET" "$base/profile" $null $authH 200

# ================================================================
#  6. ONBOARDING
# ================================================================
Write-Host "`n--- 6. ONBOARDING ---"
Test-Api "Onboarding user1" "POST" "$base/profile/onboarding" @{bio="Je suis testeur E2E"; city="Paris"; skillIds=@(); preferredDomains=@()} $authH 200
Test-Api "Onboarding user2" "POST" "$base/profile/onboarding" @{bio="Volontaire active"; city="Lyon"; skillIds=@(); preferredDomains=@()} $authH2 200

# ================================================================
#  7. PROFIL POST-ONBOARDING
# ================================================================
Write-Host "`n--- 7. PROFIL (post-onboarding) ---"
$prof = Test-Api "Get profile post-onboarding" "GET" "$base/profile" $null $authH 200
if ($prof.data) { Write-Host "    onboardingDone = $($prof.data.data.onboardingDone)" -ForegroundColor DarkGray }

# ================================================================
#  8. UPDATE PROFILE
# ================================================================
Write-Host "`n--- 8. UPDATE PROFILE ---"
Test-Api "Update profile" "PUT" "$base/profile" @{firstName="TestUpdated"; lastName="UserUpdated"; bio="Bio modifiee"; phone="0612345678"; address=@{city="Lyon"; zip="69000"}} $authH 200

# ================================================================
#  8b. UPDATE PROFILE — Vérification des données mises à jour
# ================================================================
Write-Host "`n--- 8b. VERIFICATION UPDATE PROFILE ---"
$profUp = Test-Api "Get profile after update" "GET" "$base/profile" $null $authH 200
if ($profUp.data.data.firstName -eq "TestUpdated") {
    Write-Host "    firstName correctement mis a jour" -ForegroundColor DarkGray
}

# ================================================================
#  9. CHANGE PASSWORD
# ================================================================
Write-Host "`n--- 9. CHANGE PASSWORD ---"
$newPwd = "NewTest456!@#"
Test-Api "Change password" "PUT" "$base/profile/password" @{currentPassword=$password; newPassword=$newPwd} $authH 200

# ================================================================
#  9b. CHANGE PASSWORD — Ancien mdp incorrect
# ================================================================
Write-Host "`n--- 9b. CHANGE PASSWORD (ancien mdp incorrect) ---"
Test-Api "Change password bad old pwd" "PUT" "$base/profile/password" @{currentPassword="WrongOld1!@#"; newPassword="Another1!@#"} $authH 400

# ================================================================
#  10. LOGIN AVEC NOUVEAU MDP
# ================================================================
Write-Host "`n--- 10. LOGIN nouveau mdp ---"
$login3 = Test-Api "Login new pwd" "POST" "$base/auth/login" @{email=$email; password=$newPwd} $null 200
if ($login3.data.data.accessToken) {
    $accessToken = $login3.data.data.accessToken
    $refreshToken = $login3.data.data.refreshToken
    $authH = @{ Authorization = "Bearer $accessToken" }
}

# ================================================================
#  10b. LOGIN ANCIEN MDP (doit echouer)
# ================================================================
Write-Host "`n--- 10b. LOGIN ancien mdp (doit echouer) ---"
Test-Api "Login ancien mdp" "POST" "$base/auth/login" @{email=$email; password=$password} $null 400

# ================================================================
#  11. DASHBOARD VOLUNTEER
# ================================================================
Write-Host "`n--- 11. DASHBOARD VOLUNTEER ---"
Test-Api "Volunteer dashboard" "GET" "$base/dashboard/volunteer" $null $authH 200

# ================================================================
#  12. ORGANISATIONS (publiques)
# ================================================================
Write-Host "`n--- 12. ORGANISATIONS (publiques) ---"
Test-Api "List organizations" "GET" "$base/organizations" $null $null 200
Test-Api "List organizations page 0 size 5" "GET" "$base/organizations?page=0&size=5" $null $null 200
Test-Api "List organizations search" "GET" "$base/organizations?search=test" $null $null 200

# ================================================================
#  12b. CREER ORGANISATION
# ================================================================
Write-Host "`n--- 12b. CREER ORGANISATION ---"
$orgBody = @{
    name="ONG Test E2E $ts"
    description="Organisation de test creee par le script E2E"
    mission="Tester tous les endpoints"
    domain="EDUCATION"
    addressStreet="123 Rue Test"
    addressCity="Paris"
    addressZip="75001"
    email="ong_$ts@test.com"
    phone="0100000000"
    website="https://ong-test.com"
}
$orgResult = Test-Api "Create organization" "POST" "$base/organizations" $orgBody $authH 201
$orgId = $null
$orgSlug = $null
if ($orgResult.data.data) {
    $orgId = $orgResult.data.data.id
    $orgSlug = $orgResult.data.data.slug
    Write-Host "    OrgId: $orgId / Slug: $orgSlug" -ForegroundColor DarkGray
}

# Activate the organization via DB (it's created as PENDING)
if ($orgId) {
    Write-Host "    [INFO] Activation de l'org en DB..." -ForegroundColor DarkCyan
    $sqlActivate = "UPDATE organizations SET status='ACTIVE' WHERE id='$orgId';"
    docker exec ummati-postgres psql -U ummati -d ummati_db -c $sqlActivate 2>$null | Out-Null
}

# ================================================================
#  12c. GET ORGANISATION PAR SLUG
# ================================================================
Write-Host "`n--- 12c. GET ORGANISATION PAR SLUG ---"
if ($orgSlug) {
    Test-Api "Get org by slug" "GET" "$base/organizations/$orgSlug" $null $null 200
} else {
    Write-Host "  [SKIP] Pas de slug" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  12d. UPDATE ORGANISATION
# ================================================================
Write-Host "`n--- 12d. UPDATE ORGANISATION ---"
if ($orgId) {
    Test-Api "Update organization" "PUT" "$base/organizations/$orgId" @{description="Description mise a jour E2E"; mission="Mission modifiee"; phone="0199999999"} $authH 200
} else {
    Write-Host "  [SKIP] Pas d'orgId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  12e. GET ORGANISATION 404
# ================================================================
Write-Host "`n--- 12e. GET ORGANISATION (slug inexistant) ---"
Test-Api "Get org 404" "GET" "$base/organizations/slug-qui-nexiste-pas-$ts" $null $null 404

# ================================================================
#  13. EVENTS (publiques)
# ================================================================
Write-Host "`n--- 13. EVENTS (publiques) ---"
Test-Api "List events" "GET" "$base/events" $null $null 200
Test-Api "List events page 0 size 5" "GET" "$base/events?page=0&size=5" $null $null 200
Test-Api "List events filtered city" "GET" "$base/events?city=Paris" $null $null 200
Test-Api "List events sorted desc" "GET" "$base/events?sort=startDate,desc" $null $null 200

# ================================================================
#  13b. CREER EVENEMENT
# ================================================================
Write-Host "`n--- 13b. CREER EVENEMENT ---"
$futureStart = (Get-Date).AddDays(30).ToString("yyyy-MM-ddTHH:mm:ss")
$futureEnd = (Get-Date).AddDays(30).AddHours(3).ToString("yyyy-MM-ddTHH:mm:ss")
$futureDeadline = (Get-Date).AddDays(29).ToString("yyyy-MM-ddTHH:mm:ss")
$eventId = $null
if ($orgId) {
    $eventBody = @{
        title="Evenement Test E2E $ts"
        description="Description de l evenement de test"
        objectives="Valider tous les endpoints evenements"
        type="FORMATION"
        locationName="Salle Test"
        locationAddress="456 Avenue Test"
        locationCity="Paris"
        locationZip="75002"
        online=$false
        startDate=$futureStart
        endDate=$futureEnd
        registrationDeadline=$futureDeadline
        maxParticipants=50
        requiredSkillIds=@()
    }
    $evResult = Test-Api "Create event" "POST" "$base/organizations/$orgId/events" $eventBody $authH 201
    if ($evResult.data.data) {
        $eventId = $evResult.data.data.id
        Write-Host "    EventId: $eventId" -ForegroundColor DarkGray
    }
} else {
    Write-Host "  [SKIP] Pas d'orgId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13c. GET EVENEMENT DETAIL
# ================================================================
Write-Host "`n--- 13c. GET EVENEMENT DETAIL ---"
if ($eventId) {
    Test-Api "Get event detail" "GET" "$base/events/$eventId" $null $null 200
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13d. UPDATE EVENEMENT
# ================================================================
Write-Host "`n--- 13d. UPDATE EVENEMENT ---"
if ($eventId) {
    Test-Api "Update event" "PUT" "$base/events/$eventId" @{title="Evenement MAJ $ts"; description="Description mise a jour"} $authH 200
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13e. GET EVENT 404
# ================================================================
Write-Host "`n--- 13e. GET EVENT (id inexistant) ---"
Test-Api "Get event 404" "GET" "$base/events/00000000-0000-0000-0000-000000000000" $null $null 404

# ================================================================
#  13f. INSCRIPTION EVENEMENT (User 2)
# ================================================================
Write-Host "`n--- 13f. INSCRIPTION EVENEMENT (User 2) ---"
if ($eventId) {
    Test-Api "Signup event user2" "POST" "$base/events/$eventId/signups" $null $authH2 201
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13g. LISTE INSCRIPTIONS EVENEMENT (admin org = user1)
# ================================================================
Write-Host "`n--- 13g. LISTE INSCRIPTIONS EVENEMENT ---"
if ($eventId) {
    Test-Api "List event signups" "GET" "$base/events/$eventId/signups" $null $authH 200
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13h. EXPORT CSV INSCRIPTIONS
# ================================================================
Write-Host "`n--- 13h. EXPORT CSV INSCRIPTIONS ---"
if ($eventId) {
    Test-Api "Export signups CSV" "GET" "$base/events/$eventId/signups/export" $null $authH 200
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13i. ANNULATION INSCRIPTION EVENEMENT (User 2)
# ================================================================
Write-Host "`n--- 13i. ANNULATION INSCRIPTION (User 2) ---"
if ($eventId) {
    Test-Api "Cancel signup user2" "DELETE" "$base/events/$eventId/signups" $null $authH2 204
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13j. RE-INSCRIPTION EVENEMENT (User 2)
# ================================================================
Write-Host "`n--- 13j. RE-INSCRIPTION EVENEMENT (User 2) ---"
if ($eventId) {
    Test-Api "Re-signup event user2" "POST" "$base/events/$eventId/signups" $null $authH2 201
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  13k. FEEDBACKS EVENEMENT (liste, vide)
# ================================================================
Write-Host "`n--- 13k. LIST FEEDBACKS ---"
if ($eventId) {
    Test-Api "List feedbacks (empty)" "GET" "$base/events/$eventId/feedbacks" $null $null 200
} else {
    Write-Host "  [SKIP] Pas d'eventId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  14. SKILLS
# ================================================================
Write-Host "`n--- 14. SKILLS ---"
Test-Api "List all skills" "GET" "$base/skills" $null $null 200
Test-Api "Skills by search" "GET" "$base/skills?search=communication" $null $null 200

# ================================================================
#  15. NOTIFICATIONS
# ================================================================
Write-Host "`n--- 15. NOTIFICATIONS ---"
Test-Api "List notifications" "GET" "$base/notifications" $null $authH 200
Test-Api "Unread count" "GET" "$base/notifications/unread-count" $null $authH 200

# ================================================================
#  15b. NOTIFICATIONS — Mark all as read
# ================================================================
Write-Host "`n--- 15b. NOTIFICATIONS (mark all read) ---"
Test-Api "Mark all notifications read" "PATCH" "$base/notifications/read-all" $null $authH 204

# ================================================================
#  15c. NOTIFICATIONS — Vérifier unread = 0
# ================================================================
Write-Host "`n--- 15c. NOTIFICATIONS (unread apres mark all) ---"
$notifCount = Test-Api "Unread count after mark all" "GET" "$base/notifications/unread-count" $null $authH 200
if ($notifCount.data.data.count -eq 0) {
    Write-Host "    unread count = 0 (correct)" -ForegroundColor DarkGray
}

# ================================================================
#  16. MY MEMBERSHIPS
# ================================================================
Write-Host "`n--- 16. MY MEMBERSHIPS ---"
Test-Api "My memberships" "GET" "$base/profile/memberships" $null $authH 200
Test-Api "My memberships filtered" "GET" "$base/profile/memberships?status=ACTIVE" $null $authH 200

# ================================================================
#  16b. MEMBERSHIP — User 2 demande adhesion a l'org
# ================================================================
Write-Host "`n--- 16b. MEMBERSHIP (User 2 demande adhesion) ---"
$membershipId = $null
if ($orgId) {
    $memResult = Test-Api "User2 request membership" "POST" "$base/organizations/$orgId/memberships" @{motivation="Je veux aider"} $authH2 201
    if ($memResult.data.data) {
        $membershipId = $memResult.data.data.id
        Write-Host "    MembershipId: $membershipId" -ForegroundColor DarkGray
    }
} else {
    Write-Host "  [SKIP] Pas d'orgId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  16c. MEMBERSHIP — Lister les membres de l'org
# ================================================================
Write-Host "`n--- 16c. MEMBERSHIP (lister membres org) ---"
if ($orgId) {
    Test-Api "List org members" "GET" "$base/organizations/$orgId/memberships" $null $null 200
    Test-Api "List org members PENDING" "GET" "$base/organizations/$orgId/memberships?status=PENDING" $null $null 200
} else {
    Write-Host "  [SKIP] Pas d'orgId" -ForegroundColor Yellow; $skip_count += 2
}

# ================================================================
#  16d. MEMBERSHIP — Approuver (admin org = user1)
# ================================================================
Write-Host "`n--- 16d. MEMBERSHIP (approuver par admin org) ---"
if ($membershipId) {
    Test-Api "Approve membership" "PATCH" "$base/memberships/$membershipId" @{action="APPROVE"} $authH 200
} else {
    Write-Host "  [SKIP] Pas de membershipId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  17. MY SIGNUPS
# ================================================================
Write-Host "`n--- 17. MY SIGNUPS ---"
Test-Api "My signups user1" "GET" "$base/profile/signups" $null $authH 200
Test-Api "My signups user2" "GET" "$base/profile/signups" $null $authH2 200

# ================================================================
#  18. REFRESH TOKEN
# ================================================================
Write-Host "`n--- 18. REFRESH TOKEN ---"
$refreshH = @{ Authorization = "Bearer $refreshToken" }
$refreshResult = Test-Api "Refresh token" "POST" "$base/auth/refresh" $null $refreshH 200
if ($refreshResult.data.data.accessToken) {
    Write-Host "    Nouveau token recu" -ForegroundColor DarkGray
}

# ================================================================
#  18b. REFRESH TOKEN — Token invalide
# ================================================================
Write-Host "`n--- 18b. REFRESH TOKEN (invalide) ---"
$badRefreshH = @{ Authorization = "Bearer fake-refresh-token" }
Test-Api "Refresh token invalide" "POST" "$base/auth/refresh" $null $badRefreshH 400

# ================================================================
#  19. ADMIN STATS (403 pour non-admin)
# ================================================================
Write-Host "`n--- 19. ADMIN (403 expected pour non-admin) ---"
Test-Api "Admin stats (forbidden)" "GET" "$base/admin/stats" $null $authH 403
Test-Api "Admin users (forbidden)" "GET" "$base/admin/users" $null $authH 403
Test-Api "Admin organizations (forbidden)" "GET" "$base/admin/organizations" $null $authH 403

# ================================================================
#  19b. ADMIN — Sans token (403 — Spring Security)
# ================================================================
Write-Host "`n--- 19b. ADMIN (sans token = 403) ---"
Test-Api "Admin stats sans token" "GET" "$base/admin/stats" $null $null 403

# ================================================================
#  20. SWAGGER / API DOCS
# ================================================================
Write-Host "`n--- 20. SWAGGER ---"
Test-Api "Swagger API docs" "GET" "http://localhost:8080/api-docs" $null $null 200

# ================================================================
#  21. EXPORT PROFILE (RGPD)
# ================================================================
Write-Host "`n--- 21. EXPORT PROFILE (RGPD) ---"
Test-Api "Export profile RGPD" "GET" "$base/profile/export" $null $authH 200

# ================================================================
#  22. FORGOT PASSWORD
# ================================================================
Write-Host "`n--- 22. FORGOT PASSWORD ---"
Test-Api "Forgot password" "POST" "$base/auth/forgot-password" @{email=$email} $null 200
Test-Api "Forgot password email inexistant (toujours 200)" "POST" "$base/auth/forgot-password" @{email="notexist@nowhere.com"} $null 200

# ================================================================
#  22b. RESET PASSWORD — Token invalide
# ================================================================
Write-Host "`n--- 22b. RESET PASSWORD (token invalide) ---"
Test-Api "Reset password bad token" "POST" "$base/auth/reset-password" @{token="bad-token-xyz"; newPassword="Reset123!@#"} $null 400

# ================================================================
#  23. RESEND CONFIRMATION
# ================================================================
Write-Host "`n--- 23. RESEND CONFIRMATION ---"
Test-Api "Resend confirmation" "POST" "$base/auth/resend-confirmation" @{email=$email} $null 200
Test-Api "Resend confirmation email inexistant (toujours 200)" "POST" "$base/auth/resend-confirmation" @{email="ghost@nowhere.com"} $null 200

# ================================================================
#  24. ORG ADMIN DASHBOARD
# ================================================================
Write-Host "`n--- 24. ORG ADMIN DASHBOARD ---"
if ($orgId) {
    Test-Api "Org admin dashboard" "GET" "$base/dashboard/org-admin/$orgId" $null $authH 200
} else {
    Write-Host "  [SKIP] Pas d'orgId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  25. PAGINATION — Gros page number (resultat vide)
# ================================================================
Write-Host "`n--- 25. PAGINATION (page vide) ---"
Test-Api "Events page 999 (vide)" "GET" "$base/events?page=999&size=10" $null $null 200
Test-Api "Organizations page 999 (vide)" "GET" "$base/organizations?page=999&size=10" $null $null 200

# ================================================================
#  26. TOKEN EXPIRE / INVALIDE
# ================================================================
Write-Host "`n--- 26. TOKEN INVALIDE ---"
$fakeAuthH = @{ Authorization = "Bearer eyJhbGciOiJIUzI1NiJ9.fake.token" }
Test-Api "Profile avec token invalide" "GET" "$base/profile" $null $fakeAuthH 403

# ================================================================
#  27. METHODE HTTP INCORRECTE
# ================================================================
Write-Host "`n--- 27. METHODE HTTP INCORRECTE ---"
Test-Api "DELETE sur /auth/login (methode non supportee)" "DELETE" "$base/auth/login" $null $null 500

# ================================================================
#  28. ENDPOINT INEXISTANT
# ================================================================
Write-Host "`n--- 28. ENDPOINT INEXISTANT ---"
Test-Api "Endpoint inexistant (403 sans token)" "GET" "$base/this-does-not-exist" $null $null 403

# ================================================================
#  29. SUPPRESSION MEMBERSHIP
# ================================================================
Write-Host "`n--- 29. SUPPRESSION MEMBERSHIP ---"
if ($membershipId) {
    Test-Api "Remove membership" "DELETE" "$base/memberships/$membershipId" $null $authH 204
} else {
    Write-Host "  [SKIP] Pas de membershipId" -ForegroundColor Yellow; $skip_count++
}

# ================================================================
#  30. DELETE ACCOUNT (User 2)
# ================================================================
Write-Host "`n--- 30. DELETE ACCOUNT (User 2) ---"
Test-Api "Delete account user2" "DELETE" "$base/profile" @{password=$password} $authH2 204

# ================================================================
#  30b. LOGIN APRES SUPPRESSION (doit echouer)
# ================================================================
Write-Host "`n--- 30b. LOGIN APRES SUPPRESSION (User 2) ---"
Test-Api "Login user2 after delete" "POST" "$base/auth/login" @{email=$email2; password=$password} $null 400

# ================================================================
#  31. LOGOUT (204 = NoContent)
# ================================================================
Write-Host "`n--- 31. LOGOUT ---"
Test-Api "Logout" "POST" "$base/auth/logout" $null $authH 204

# ================================================================
#  RAPPORT FINAL
# ================================================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  RAPPORT FINAL" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
$total = $pass_count + $fail_count + $skip_count
Write-Host "  Total:  $total tests" -ForegroundColor White
Write-Host "  PASS:   $pass_count" -ForegroundColor Green
Write-Host "  FAIL:   $fail_count" -ForegroundColor $(if ($fail_count -gt 0) { "Red" } else { "Green" })
Write-Host "  SKIP:   $skip_count" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
if ($fail_count -eq 0) {
    Write-Host "  ALL TESTS PASSED!" -ForegroundColor Green
} else {
    Write-Host "  $fail_count TEST(S) FAILED" -ForegroundColor Red
}
Write-Host ""


