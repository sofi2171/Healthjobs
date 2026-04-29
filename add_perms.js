const fs = require('fs');
const path = './android/app/src/main/AndroidManifest.xml';

try {
  let content = fs.readFileSync(path, 'utf8');
  
  const perms = `
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
`;

  // Check if permissions already exist
  if (!content.includes('android.permission.RECORD_AUDIO')) {
    // Insert right before the <application tag safely
    content = content.replace('<application', perms + '\n    <application');
    fs.writeFileSync(path, content);
    console.log('✅ Success: Permissions automatically added to AndroidManifest.xml!');
  } else {
    console.log('⚠️ Notice: Permissions are already present in the file.');
  }
} catch (error) {
  console.log('❌ Error: AndroidManifest.xml nahi mili. Kya aap project folder mein hain?');
}
