const fs = require('node:fs');
const dir = '.archify/architecture-veggiepal-workflow-20261027-114048';
const repository = {url:'https://github.com/Phu0312/VeggiePal_BE.git',revision:'21a45e9ec09316fd5293151ad891ef990c45e159',link_mode:'web'};
const source = (path,line,end_line) => ({path,line,end_line});
const gateway = 'api-gateway/src/main/resources/application.properties';
const n = 'nutrition-service/src/main/java/com/veggiepal/nutrition/';
const meta = (title,name) => ({title,locale:'vi',quality_profile:'showcase',repository,output:`${dir}/${name}.html`});
const services = [
 ['identity','identity-service',':8081 · Tài khoản, hồ sơ',source('identity-service/src/main/java/com/veggiepal/service/UserService.java',39,105),'/auth · /users'],
 ['nutrition','nutrition-service',':8082 · Sức khỏe, dị ứng',source(n+'service/HealthRecordService.java',38,101),'/nutrition'],
 ['blog','blog-service',':8083 · Bài viết, video',source('blog-service/src/main/java/com/veggiepal/blog/service/BlogService.java',61,108),'/blogs · /videos…'],
 ['recipe','recipe-service',':8084 · Công thức, nguyên liệu',source('recipe-service/src/main/java/com/veggiepal/recipe/service/RecipeService.java',57,151),'/recipes · /ingredients'],
 ['location','location-service',':8085 · Quán chay gần đây',source('location-service/src/main/java/com/veggiepal/location/service/RestaurantService.java',27,58),'/restaurants'],
 ['ai','ai-service',':8086 · Chat, thực đơn, AI',source('ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java',88,139),'/ai'],
 ['payment','payment-service',':8087 · Thanh toán, gói dùng',source('payment-service/src/main/java/com/veggiepal/payment/service/PaymentService.java',104,132),'/payments · /subscriptions…']
];
const architecture = {
 schema_version:1,diagram_type:'architecture',meta:meta('VeggiePal · Cổng vào và 7 service nghiệp vụ','ban-do-service'),
 components:[
 {id:'client',type:'external',label:'Client HTTP',sublabel:'Web, app hoặc Swagger',pos:[40,350],size:[180,80],sources:[source(gateway,20,28)]},
 {id:'gateway',type:'backend',label:'api-gateway :8080',sublabel:'Chọn route theo URL',tag:'Bỏ /api bằng StripPrefix=1',pos:[330,300],size:[220,180],sources:[source(gateway,1,114),source(gateway,127,153)]},
 ...services.map(([id,label,sublabel,s],i)=>({id,type:'backend',label,sublabel,pos:[800,40+i*110],size:[240,74],sources:[s]}))
 ],
 connections:[{id:'client-gateway',from:'client',to:'gateway',label:'HTTP',variant:'emphasis'},...services.map(([id,,,,label])=>({id:'route-'+id,from:'gateway',to:id,label,variant:'default'}))],
 cards:[
 {dot:'cyan',title:'Đọc bản đồ',items:['Mỗi service là một Maven project và ứng dụng Spring Boot độc lập.','Cổng là mặc định cấu hình; bản đồ không khẳng định các tiến trình đang chạy.','Đây là bản đồ định tuyến. Các lời gọi giữa service được giải thích trong bài hướng dẫn.']},
 {dot:'emerald',title:'Dữ liệu thuộc về ai?',items:['7 service nghiệp vụ dùng 7 schema MySQL riêng; Gateway không quản lý dữ liệu nghiệp vụ.','Cùng một máy MySQL vẫn có thể chứa nhiều schema. Không suy ra 7 máy database.','DTO là dữ liệu API; entity là dữ liệu lưu; mapper chuyển đổi giữa chúng.']},
 {dot:'rose',title:'Hai điểm dễ nhầm',items:['Spring Security ở service bảo vệ API; Gateway chủ yếu định tuyến và cấu hình CORS.','AI còn nhận /ai/** trực tiếp qua Gateway mà không bỏ prefix; /api/ai/** thì bỏ /api.','Nội dung sơ đồ bằng tiếng Việt; một số nút điều khiển Viewer có thể dùng tiếng Anh.']}
 ]
};
const wfNode=(id,lane,col,type,label,sublabel,sources)=>({id,lane,col,type,label,sublabel,sources});
const workflow = {
 schema_version:2,diagram_type:'workflow',meta:meta('VeggiePal · Một request ghi sức khỏe đi qua những lớp nào?','workflow-ghi-suc-khoe'),
 lanes:[{id:'request',label:'Tiếp nhận và xử lý'},{id:'result',label:'Lưu dữ liệu và trả kết quả'}],
 mainPath:['request','route','auth','controller','service','repository','database'],
 nodes:[
 wfNode('request','request',0,'external','Gửi request','POST /api/nutrition/…',[source(gateway,38,41)]),
 wfNode('route','request',1,'backend','Gateway','Bỏ /api, chuyển :8082',[source(gateway,10,10),source(gateway,38,41)]),
 wfNode('auth','request',2,'security','Spring Security','Xác thực Bearer JWT',[source(n+'configuration/SecurityConfig.java',39,73)]),
 wfNode('controller','request',3,'backend','DTO + Controller','@Valid · lấy userId',[source(n+'controller/HealthRecordController.java',23,44),source(n+'dto/request/HealthRecordRequest.java',18,30)]),
 wfNode('service','request',4,'backend','HealthRecordService','Tính BMI, tạo entity',[source(n+'service/HealthRecordService.java',38,49),source(n+'service/HealthRecordService.java',104,109)]),
 wfNode('repository','request',5,'backend','JPA Repository','save(healthRecord)',[source(n+'repository/HealthRecordRepository.java',12,19),source(n+'service/HealthRecordService.java',38,49)]),
 wfNode('database','result',5,'database','MySQL','Bảng health_records',[source(n+'entity/HealthRecord.java',16,47),source('nutrition-service/src/main/resources/application.properties',5,9)]),
 wfNode('mapper','result',4,'backend','Service gọi mapper','Entity → Response DTO',[source(n+'service/HealthRecordService.java',38,49),source(n+'mapper/HealthRecordMapper.java',8,11)]),
 wfNode('response','result',3,'backend','JSON cho client','ApiResponse · code 1000',[source(n+'controller/HealthRecordController.java',35,44),source(n+'dto/response/ApiResponse.java',8,21),source(n+'exception/GlobalExceptionHandler.java',119,130)]),
 wfNode('error','result',2,'security','Phản hồi lỗi','401 hoặc lỗi validation',[source(n+'configuration/SecurityConfig.java',53,63),source(n+'exception/GlobalExceptionHandler.java',67,116)])
 ],
 edges:[
 {from:'request',to:'route',role:'main'},
 {from:'route',to:'auth',role:'main'},
 {from:'auth',to:'controller',label:'Hợp lệ',role:'main'},
 {from:'controller',to:'service',label:'DTO hợp lệ',role:'main'},
 {from:'service',to:'repository',role:'main'},
 {from:'repository',to:'database',label:'Ghi SQL',role:'main'},
 {from:'database',to:'mapper',label:'Entity đã lưu',role:'return'},
 {from:'mapper',to:'response',role:'return'},
 {from:'auth',to:'error',label:'JWT lỗi',variant:'security',role:'error'},
 {from:'controller',to:'error',label:'DTO lỗi',variant:'security',role:'error'}
 ],
 cards:[
 {dot:'cyan',title:'Ví dụ để tự lần code',items:['Body: {"heightCm":170,"weightKg":65}. userId lấy từ JWT, không lấy từ body.','Service tính 65 / (1.7 × 1.7), làm tròn HALF_UP một chữ số: BMI = 22.5.','Đọc source từ Controller → Service → Repository, rồi quay lại Mapper → ApiResponse.']},
 {dot:'emerald',title:'Đường quay về được rút gọn',items:['Database không gọi mapper trực tiếp: service nhận entity sau save rồi gọi mapper.','Response được Controller bọc bằng ApiResponse và trả qua Gateway tới client.','HealthRecordRequest được Spring kiểm tra trước khi chạy thân method Controller.']},
 {dot:'rose',title:'Hai nơi xử lý lỗi',items:['SecurityExceptionHandler xử lý lỗi xác thực/phân quyền trong filter chain.','GlobalExceptionHandler chuyển lỗi DTO hoặc AppException thành JSON và HTTP status.','Nhánh lỗi là kết thúc request; không tiếp tục ghi database.']}
 ]
};
fs.writeFileSync(`${dir}/candidate.json`,JSON.stringify(architecture,null,2));
fs.writeFileSync(`${dir}/workflow.candidate.json`,JSON.stringify(workflow,null,2));
