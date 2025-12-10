$baseUrl = "http://localhost:8087"

# 1. Create second profile
Write-Host "Creating second profile..." -ForegroundColor Cyan
$profile2 = @{
    userId = 2
    userName = "PomeLover"
    userAvatar = "/woman-profile.png"
    userGender = "Female"
    petName = "MungChi"
    petBreed = "Pomeranian"
    petAge = 2
    petGender = "Female"
    petPhoto = "/cute-pomeranian.png"
    bio = "I love walking with my Pomeranian!"
    activityLevel = 75
    latitude = 37.5665
    longitude = 126.9780
    location = "Seoul Gangnam"
}

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/api/petmate/profile" -Method Post -ContentType "application/json" -Body ($profile2 | ConvertTo-Json -Depth 10)
    Write-Host "SUCCESS: Profile created!" -ForegroundColor Green
    $result | ConvertTo-Json | Write-Host
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Create chat room between user 1 and 2
Write-Host "`nCreating chat room..." -ForegroundColor Cyan
try {
    $chatRoom = Invoke-RestMethod -Uri "$baseUrl/api/messages/room?userId1=1&userId2=2" -Method Post
    Write-Host "SUCCESS: Chat room created! ID: $($chatRoom.id)" -ForegroundColor Green
    $chatRoomId = $chatRoom.id
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    $chatRoomId = 1
}

# 3. Send a message from user 1
Write-Host "`nSending message from user 1..." -ForegroundColor Cyan
$message1 = @{
    chatRoomId = $chatRoomId
    senderId = 1
    content = "Hello! Nice to meet you!"
    messageType = "TEXT"
}

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/api/messages/send" -Method Post -ContentType "application/json" -Body ($message1 | ConvertTo-Json -Depth 10)
    Write-Host "SUCCESS: Message sent from user 1!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Send a message from user 2
Write-Host "`nSending message from user 2..." -ForegroundColor Cyan
$message2 = @{
    chatRoomId = $chatRoomId
    senderId = 2
    content = "Nice to meet you too! Want to walk together?"
    messageType = "TEXT"
}

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/api/messages/send" -Method Post -ContentType "application/json" -Body ($message2 | ConvertTo-Json -Depth 10)
    Write-Host "SUCCESS: Message sent from user 2!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Get all messages
Write-Host "`nRetrieving messages..." -ForegroundColor Cyan
try {
    $messages = Invoke-RestMethod -Uri "$baseUrl/api/messages/room/$chatRoomId`?userId=1" -Method Get
    Write-Host "SUCCESS: Found $($messages.Count) messages!" -ForegroundColor Green
    $messages | ForEach-Object {
        Write-Host "  [$($_.senderName)]: $($_.content)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nDone!" -ForegroundColor Green
