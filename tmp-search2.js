const fs=require("fs");
const mod="C:/Users/20930/Desktop/frontend_from_docker/html/assets/modules-67c10402.js";
const text=fs.readFileSync(mod,"utf8");
const keys=["createTeacherQuestion","updateTeacherQuestion","deleteTeacherQuestion","getTeacherQuestion","TeacherQuestion","/questions","testCases","testCase"];
for (const p of keys) {
  let idx=0, count=0;
  while ((idx=text.indexOf(p, idx))>=0 && count<15) {
    const start=Math.max(0, idx-150);
    const end=Math.min(text.length, idx+p.length+250);
    console.log("\n========== modules | "+p+" @ "+idx+" ==========");
    console.log(text.slice(start, end));
    idx += p.length; count++;
  }
}
