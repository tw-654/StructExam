const fs=require("fs");
const path="C:/Users/20930/Desktop/frontend_from_docker/html/assets/TeacherDashboard-164c7cef.js";
const text=fs.readFileSync(path,"utf8");
const patterns=["测试用例","testCases","expectedOutput","保存","新增","options","saveQuestion","Y.create","Y.update","batch","inputData","weight","score","isPublic","createTeacherQuestion","updateTeacherQuestion","deleteTeacherQuestion","TeacherQuestion"];
for (const p of patterns) {
  let idx=0, count=0;
  while ((idx=text.indexOf(p, idx))>=0 && count<10) {
    const start=Math.max(0, idx-200);
    const end=Math.min(text.length, idx+p.length+200);
    console.log("\n========== "+p+" occ "+(count+1)+" at "+idx+" ==========");
    console.log(text.slice(start, end));
    idx += p.length; count++;
  }
  if (count===0) console.log("\n========== "+p+" NO MATCH ==========");
}
const neIdx = text.indexOf("ne=async");
if (neIdx>=0) console.log("\n========== ne=async block ==========\n"+text.slice(neIdx, neIdx+900));
console.log("\n========== file start (imports) ==========\n"+text.slice(0,500));
