echo ** Security Test Script for TC_SEC_003 **
echo.
echo Make sure your Spring Boot application is running before you continue.
pause
echo.

echo ** Step 1: Registering a new teacher user... **
curl -X POST -H "Content-Type: application/json" -d "{\"username\": \"securityteacher3\", \"email\": \"security3@example.com\", \"password\": \"password\", \"role\": \"TEACHER\"}" http://localhost:8080/api/auth/register
echo.
echo.
echo ** Step 2: Logging in and saving the session cookie to cookie-jar.txt... **
curl -X POST -H "Content-Type: application/json" -d "{\"username\": \"securityteacher3\", \"password\": \"password\"}" -c cookie-jar.txt http://localhost:8080/api/auth/login
echo.
echo.
echo ** Step 3: Sending the malicious request... **
echo This is the actual test. We expect to see an HTTP 400 Bad Request status.
curl -i -b cookie-jar.txt "http://localhost:8080/api/teacher/absence-requests/status/%27APPROVED%27%20OR%201=1"
echo.
echo.
echo ** Test Complete **
echo Look at the output of the last command. The first line should be "HTTP/1.1 400 Bad Request".
del cookie-jar.txt
pause
