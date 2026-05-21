const fs=require("fs");
const td=fs.readFileSync("C:/Users/20930/Desktop/frontend_from_docker/html/assets/TeacherDashboard-164c7cef.js","utf8");
const mod=fs.readFileSync("C:/Users/20930/Desktop/frontend_from_docker/html/assets/modules-67c10402.js","utf8");
const tIdx = td.indexOf("T=async");
console.log("=== T=async ===\n"+td.slice(tIdx, tIdx+600));
const jIdx = td.indexOf("j=(s=null)");
console.log("\n=== j= opener ===\n"+td.slice(jIdx, jIdx+400));
console.log("\n=== modules exports (tail) ===\n"+mod.slice(mod.length-1200));
for (const p of ["baseURL","/api"]) {
  let i=0,c=0;
  while ((i=mod.indexOf(p,i))>=0 && c<3) {
    console.log("\n=== "+p+" @ "+i+" ===\n"+mod.slice(Math.max(0,i-80), i+200));
    i+=p.length;c++;
  }
}
