Write-Host "=== Reset state ==="
docker exec postgres psql -U root -d assessmentdb -c "DELETE FROM comments WHERE post_id=3;" | Out-Null
docker exec redis redis-cli FLUSHDB | Out-Null

Write-Host "=== Firing 200 concurrent bot comments ==="

$processes = @()
for ($i = 1; $i -le 200; $i++) {
    $body = "{\""authorId\"":$i,\""authorType\"":\""BOT\"",\""content\"":\""Bot $i\"",\""depthLevel\"":0,\""humanAuthorId\"":1}"
    $args = "-s -X POST http://localhost:8080/api/posts/3/comments -H `"Content-Type: application/json`" -d `"$body`""
    $processes += Start-Process -FilePath "curl.exe" -ArgumentList $args -NoNewWindow -PassThru
}

Write-Host "Waiting..."
$processes | Wait-Process

Write-Host "`n=== Results ==="
Write-Host "DB bot comments (MUST be 100):"
docker exec postgres psql -U root -d assessmentdb -c "SELECT COUNT(*) FROM comments WHERE post_id=3 AND author_type='BOT';"
Write-Host "Redis bot_count (MUST be 100):"
docker exec redis redis-cli GET "post:3:bot_count"