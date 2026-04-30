const fs = require('fs');
const path = './android/app/src/main/AndroidManifest.xml';

try {
  let content = fs.readFileSync(path, 'utf8');
  
  const perms = `
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
`;

  if (!content.includes('USE_FULL_SCREEN_INTENT')) {
    content = content.replace('<application', perms + '\n    <application');
    fs.writeFileSync(path, content);
    console.log('✅ Success: Call & Wake-up permissions added to AndroidManifest.xml!');
  } else {
    console.log('⚠️ Notice: Call Permissions are already present.');
  }
} catch (error) {
  console.log('❌ Error: AndroidManifest.xml nahi mili.');
}
