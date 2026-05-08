// firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyD4Cfni7D2Kk_t6qeZ4jcWesIabnSM15mk",
    authDomain: "jobs-45cc9.firebaseapp.com",
    projectId: "jobs-45cc9",
    storageBucket: "jobs-45cc9.firebasestorage.app",
    messagingSenderId: "21065686301",
    appId: "1:21065686301:web:f461ea1b8aabe2fa5895f4"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    console.log('📩 Background:', payload);
    
    const { notification = {}, data = {} } = payload;
    
    self.registration.showNotification(
        notification.title || 'Health Jobs Alert',
        {
            body: notification.body || 'New medical jobs available!',
            icon: '/images/logo.png',
            badge: '/images/favicon.png',
            data: data,
            vibrate: [200, 100, 200],
            tag: data.postId || 'post',
            renotify: true
        }
    );
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const data = event.notification.data || {};
    let url = '/';
    if (data.postId) url = `/details.html?id=${data.postId}`;
    
    event.waitUntil(
        clients.matchAll({ type: 'window' }).then(clientList => {
            for (const client of clientList) {
                if (client.url.includes(url) && 'focus' in client) return client.focus();
            }
            if (clients.openWindow) return clients.openWindow(url);
        })
    );
});
