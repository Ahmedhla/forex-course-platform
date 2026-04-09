// Auth Modal Handling
const modal = document.getElementById('authModal');
const authBtn = document.getElementById('authBtn');

authBtn.onclick = (e) => {
    e.preventDefault();
    if (localStorage.getItem('token')) {
        localStorage.clear();
        window.location.reload();
    } else {
        modal.style.display = 'block';
    }
};

document.querySelector('.close').onclick = () => {
    modal.style.display = 'none';
};

window.onclick = (e) => {
    if (e.target === modal) modal.style.display = 'none';
};

async function login() {
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const result = await api.login({ email, password });
        localStorage.setItem('token', result.token);
        localStorage.setItem('role', result.role);
        localStorage.setItem('fullName', result.fullName);
        modal.style.display = 'none';
        window.location.reload();
    } catch (error) {
        alert('Login failed: ' + (error.message || 'Invalid credentials'));
    }
}

async function register() {
    const fullName = document.getElementById('regFullName').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;

    try {
        const result = await api.register({ fullName, email, password });
        localStorage.setItem('token', result.token);
        localStorage.setItem('role', result.role);
        localStorage.setItem('fullName', result.fullName);
        modal.style.display = 'none';
        window.location.reload();
    } catch (error) {
        alert('Registration failed: ' + (error.message || 'Email may already exist'));
    }
}

function showRegister() {
    document.getElementById('loginForm').style.display = 'none';
    document.getElementById('registerForm').style.display = 'block';
}

function showLogin() {
    document.getElementById('loginForm').style.display = 'block';
    document.getElementById('registerForm').style.display = 'none';
}