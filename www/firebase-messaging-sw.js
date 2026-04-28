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

// Background mein notification handle karne ke liye
messaging.onBackgroundMessage((payload) => {
    console.log('Background Message received: ', payload);
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '/images/logo.png'
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});
