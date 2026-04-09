// Use relative paths since frontend and backend are on same server
const API_BASE = '/api';  // NOT http://localhost:8080/api

const api = {
    async request(endpoint, options = {}) {
        const token = localStorage.getItem('token');
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers
        });

        if (response.status === 401) {
            localStorage.clear();
            window.location.href = '/index.html';
            throw new Error('Unauthorized');
        }

        return response.json();
    },

    // Auth endpoints
    login(data) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    register(data) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    // Course endpoints
    getPublicCourses() {
        return this.request('/courses/public');
    },

    getMyCourses() {
        return this.request('/courses/my-courses');
    },

    purchaseCourse(courseId) {
        return this.request(`/courses/purchase/${courseId}`, {
            method: 'POST'
        });
    },

    // Progress endpoints
    updateProgress(videoId, position) {
        return this.request(`/courses/progress/${videoId}`, {
            method: 'POST',
            body: JSON.stringify({ position })
        });
    },

    getProgress(videoId) {
        return this.request(`/courses/progress/${videoId}`);
    },

    // Admin endpoints
    createCourse(course) {
        return this.request('/admin/courses', {
            method: 'POST',
            body: JSON.stringify(course)
        });
    },

    deleteCourse(courseId) {
        return this.request(`/admin/courses/${courseId}`, {
            method: 'DELETE'
        });
    },

    addVideoToCourse(courseId, video) {
        return this.request(`/admin/courses/${courseId}/videos`, {
            method: 'POST',
            body: JSON.stringify(video)
        });
    },

    async uploadVideo(file) {
        const formData = new FormData();
        formData.append('file', file);

        const token = localStorage.getItem('token');
        const response = await fetch('/api/admin/upload/video', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });

        return response.json();
    },

    async uploadThumbnail(file) {
        const formData = new FormData();
        formData.append('file', file);

        const token = localStorage.getItem('token');
        const response = await fetch('/api/admin/upload/thumbnail', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });

        return response.json();
    }
};