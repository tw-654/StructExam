const fs=require("fs");
const exam="C:/Users/20930/Desktop/frontend_from_docker/html/assets/Exam-3d07adba.js";
const text=fs.readFileSync(exam,"utf8");
const keys=["testCases","expectedOutput","inputData","isPublic","createTeacherQuestion","/teacher/questions","/questions/","TeacherQuestion","test-case","testCase"];
for (const p of keys) {
  let idx=0, count=0;
  while ((idx=text.indexOf(p, idx))>=0 && count<5) {
    const start=Math.max(0, idx-120);
    const end=Math.min(text.length, idx+p.length+180);
    console.log("\n========== Exam | "+p+" @ "+idx+" ==========");
    console.log(text.slice(start, end));
    idx += p.length; count++;
  }
  if (count===0) console.log("\n========== Exam | "+p+" NO MATCH ==========");
}
