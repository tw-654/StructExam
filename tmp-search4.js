const fs=require("fs");
const text=fs.readFileSync("C:/Users/20930/Desktop/frontend_from_docker/html/assets/TeacherDashboard-164c7cef.js","utf8");
for (const p of ["新增","添加","题目","j=","onClick:ne","getTeacherQuestions","listTeacher"]) {
  let idx=0, c=0;
  while ((idx=text.indexOf(p, idx))>=0 && c<8) {
    console.log("\n--- "+p+" @ "+idx+" ---");
    console.log(text.slice(Math.max(0,idx-120), Math.min(text.length, idx+p.length+180)));
    idx+=p.length; c++;
  }
}
