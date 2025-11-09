// Global state
let currentToken = null;
let currentApiKey = null;

// Tab switching
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });

    // Remove active class from all tab buttons
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // Show selected tab
    document.getElementById(tabName).classList.add('active');

    // Add active class to clicked tab button
    event.target.classList.add('active');
}

// Quick fill credentials
function quickFill(username, password) {
    document.getElementById('jwt-username').value = username;
    document.getElementById('jwt-password').value = password;
}

function quickFillBasic(username, password) {
    document.getElementById('basic-username').value = username;
    document.getElementById('basic-password').value = password;
}

// JWT Login
async function jwtLogin() {
    const username = document.getElementById('jwt-username').value;
    const password = document.getElementById('jwt-password').value;

    if (!username || !password) {
        showResponse('jwt-response', 'error', 'Please enter both username and password');
        return;
    }

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok) {
            currentToken = data.token;
            document.getElementById('jwt-test-btn').disabled = false;

            const message = `
                <strong>✅ Login Successful!</strong><br>
                <strong>User:</strong> ${data.user.username} (${data.user.displayName})<br>
                <strong>Roles:</strong> ${Array.from(data.user.roles).join(', ')}<br>
                <strong>Token Type:</strong> ${data.tokenType}<br>
                <strong>Expires In:</strong> ${data.expiresIn} seconds<br>
                <strong>Token:</strong> <code style="word-break: break-all;">${data.token.substring(0, 50)}...</code><br>
                <div style="margin-top: 10px; padding: 10px; background: #d4edda; border-radius: 3px;">
                    💡 Token saved! You can now test protected endpoints.
                </div>
            `;
            showResponse('jwt-response', 'success', message);
        } else {
            showResponse('jwt-response', 'error', data.message || 'Login failed');
        }
    } catch (error) {
        showResponse('jwt-response', 'error', 'Error: ' + error.message);
    }
}

// Test JWT Token
async function testJwtToken() {
    if (!currentToken) {
        showResponse('jwt-response', 'error', 'Please login first to get a token');
        return;
    }

    try {
        const response = await fetch('/api/me', {
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        const data = await response.json();

        if (response.ok) {
            const message = `
                <strong>✅ Token Valid!</strong><br>
                <pre>${JSON.stringify(data, null, 2)}</pre>
            `;
            showResponse('jwt-response', 'success', message);
        } else {
            showResponse('jwt-response', 'error', 'Token validation failed');
        }
    } catch (error) {
        showResponse('jwt-response', 'error', 'Error: ' + error.message);
    }
}

// Test API Key
async function testApiKey() {
    const apiKey = document.getElementById('apikey-value').value;
    const location = document.getElementById('apikey-location').value;

    if (!apiKey) {
        showResponse('apikey-response', 'error', 'Please enter an API key');
        return;
    }

    try {
        let response;

        if (location === 'header') {
            response = await fetch('/api/me', {
                headers: {
                    'X-API-Key': apiKey
                }
            });
        } else {
            response = await fetch(`/api/me?api_key=${apiKey}`);
        }

        const data = await response.json();

        if (response.ok) {
            currentApiKey = apiKey;
            const message = `
                <strong>✅ API Key Valid!</strong><br>
                <strong>Method:</strong> ${location === 'header' ? 'Header (X-API-Key)' : 'Query Parameter'}<br>
                <pre>${JSON.stringify(data, null, 2)}</pre>
            `;
            showResponse('apikey-response', 'success', message);
        } else {
            showResponse('apikey-response', 'error', 'Invalid API key');
        }
    } catch (error) {
        showResponse('apikey-response', 'error', 'Error: ' + error.message);
    }
}

// Test Basic Auth
async function testBasicAuth() {
    const username = document.getElementById('basic-username').value;
    const password = document.getElementById('basic-password').value;

    if (!username || !password) {
        showResponse('basic-response', 'error', 'Please enter both username and password');
        return;
    }

    try {
        // Create Basic Auth header
        const credentials = btoa(`${username}:${password}`);

        const response = await fetch('/api/me', {
            headers: {
                'Authorization': `Basic ${credentials}`
            }
        });

        const data = await response.json();

        if (response.ok) {
            const message = `
                <strong>✅ Basic Auth Successful!</strong><br>
                <strong>Credentials:</strong> ${username}:******<br>
                <strong>Encoded:</strong> <code>${credentials.substring(0, 20)}...</code><br>
                <pre>${JSON.stringify(data, null, 2)}</pre>
            `;
            showResponse('basic-response', 'success', message);
        } else {
            showResponse('basic-response', 'error', 'Authentication failed - Invalid credentials');
        }
    } catch (error) {
        showResponse('basic-response', 'error', 'Error: ' + error.message);
    }
}

// Test endpoint
async function testEndpoint(url, method) {
    try {
        const options = {
            method: method
        };

        // Add authentication if available
        if (currentToken) {
            options.headers = {
                'Authorization': `Bearer ${currentToken}`
            };
        } else if (currentApiKey) {
            options.headers = {
                'X-API-Key': currentApiKey
            };
        }

        const response = await fetch(url, options);
        const data = await response.json();

        let message;
        if (response.ok) {
            message = `
                <strong>✅ Success!</strong><br>
                <strong>Endpoint:</strong> ${method} ${url}<br>
                <strong>Status:</strong> ${response.status} ${response.statusText}<br>
                <pre>${JSON.stringify(data, null, 2)}</pre>
            `;
            showResponse('test-response', 'success', message);
        } else {
            message = `
                <strong>❌ Failed!</strong><br>
                <strong>Endpoint:</strong> ${method} ${url}<br>
                <strong>Status:</strong> ${response.status} ${response.statusText}<br>
                <strong>Message:</strong> ${data.message || 'Access denied'}<br>
                <div style="margin-top: 10px; padding: 10px; background: rgba(255, 255, 255, 0.5); border-radius: 3px;">
                    💡 This might be due to:<br>
                    - No authentication provided<br>
                    - Insufficient permissions<br>
                    - Missing required role
                </div>
            `;
            showResponse('test-response', 'error', message);
        }
    } catch (error) {
        showResponse('test-response', 'error', 'Error: ' + error.message);
    }
}

// Show response
function showResponse(elementId, type, message) {
    const element = document.getElementById(elementId);
    element.className = `response ${type}`;
    element.innerHTML = message;
    element.style.display = 'block';

    // Scroll to response
    element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// Load test credentials on page load
window.addEventListener('DOMContentLoaded', async () => {
    console.log('Aegis Demo App loaded');

    // Test if backend is running
    try {
        const response = await fetch('/api/health');
        if (response.ok) {
            console.log('✅ Backend is running');
        }
    } catch (error) {
        console.error('❌ Backend is not accessible:', error);
        alert('Warning: Could not connect to backend server. Please ensure the application is running.');
    }
});

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // Enter key on login forms
    if (e.key === 'Enter') {
        const activeTab = document.querySelector('.tab-content.active');
        if (activeTab.id === 'jwt') {
            jwtLogin();
        } else if (activeTab.id === 'basic') {
            testBasicAuth();
        } else if (activeTab.id === 'apikey') {
            testApiKey();
        }
    }
});
