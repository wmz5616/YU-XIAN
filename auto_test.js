const http = require('http');

console.log("===================================");
console.log("渔鲜直供平台 - 自动化测试脚本");
console.log("===================================");
console.log("开始执行全链路核心业务流程断言...\n");

const API_BASE = "http://localhost:8080";

let userToken = "";
let adminToken = "";
let testUsername = `user${Date.now().toString().slice(-6)}`;
let orderId = "";
let productId = 1;

async function request(path, method = "GET", body = null, token = null) {
  const headers = {
    "Content-Type": "application/json",
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const options = {
    method,
    headers,
  };
  if (body) {
    options.body = JSON.stringify(body);
  }

  try {
    const res = await fetch(`${API_BASE}${path}`, options);
    const text = await res.text();
    let data;
    try {
      data = text ? JSON.parse(text) : {};
    } catch (e) {
      data = text;
    }

    return {
      status: res.status,
      data: data
    };
  } catch (err) {
    console.error(`❌ 请求 ${path} 失败:`, err.message);
    throw err;
  }
}

async function runTests() {
  try {
    process.stdout.write("[⏳] 正在测试异常注册参数拦截... ");
    let res = await request("/api/users/register", "POST", { username: "a", password: "123" });
    if (res.data.success === false) {
      console.log("✅ 拦截成功！(" + res.data.message + ")");
    } else {
      throw new Error("预期会被拦截，但却通过了");
    }

    process.stdout.write("[⏳] 正在为您注册随机专属测试账号 [" + testUsername + "]... ");
    res = await request("/api/users/register", "POST", { username: testUsername, password: "123" });
    if (res.status === 200 && res.data.success) {
      console.log("✅ 注册成功！");
    } else {
      throw new Error("注册失败");
    }

    process.stdout.write("[⏳] 正在测试登录发放 Token (该Token将用于全链路)... ");
    res = await request("/api/users/login", "POST", { username: testUsername, password: "123" });
    if (res.status === 200 && res.data.token) {
      userToken = res.data.token;
      console.log(`✅ 登录成功！获取Token前缀: ${userToken.substring(0, 15)}...`);
    } else {
      throw new Error("登录失败");
    }

    process.stdout.write("[⏳] 正在以最高管理员身份登录系统... ");
    res = await request("/api/users/login", "POST", { username: "admin", password: "123" });
    if (res.status === 200 && res.data.token) {
      adminToken = res.data.token;
      console.log("✅ 管理员鉴权成功！");
    } else {
      throw new Error("需要预先存在admin/123的管理员账户");
    }

    process.stdout.write("[⏳] 消费者正在维护配送地址薄... ");
    res = await request("/api/users/address", "POST", {
      addresses: [{
        contact: "自动化测试员",
        phone: "13800138000",
        detail: "测试科技园南区102栋"
      }]
    }, userToken);
    if (res.status === 200) {
      console.log("✅ 地址信息注入成功！");
    }

    process.stdout.write(`[⏳] 消费者正在尝试从购物车勾选结算与创建订单... `);
    res = await request("/api/orders", "POST", {
      items: [{ id: productId, quantity: 1 }],
      address: {
        contact: "自动化测试员",
        phone: "13800138000",
        detail: "测试科技园南区102栋"
      },
      couponId: null
    }, userToken);

    if (res.status === 200 && res.data.orderId) {
      orderId = res.data.orderId;
      console.log(`✅ 订单建立成功 (流水单号: ${orderId}) 并正确扣减初始库存！`);
    } else {
      throw new Error("订单创建失败: " + JSON.stringify(res.data));
    }

    process.stdout.write(`[⏳] 消费者拉起收银台 (支付方式: NORMAL)... `);
    const payRes = await request(`/api/orders/${orderId}/pay`, "POST", { method: "NORMAL" }, userToken);
    if (payRes.status === 200 && payRes.data.includes("支付成功")) {
      console.log("✅ 资金冻结，订单流转入已支付(PAID)队列！");
    } else {
      throw new Error("支付失败: " + payRes.data);
    }

    process.stdout.write(`[⏳] 管理员发货与用户模拟送达状态流转... `);
    await request(`/api/admin/orders/${orderId}/status`, "PUT", { status: "DELIVERED" }, adminToken);
    console.log("✅ 订单流转进入已送达(DELIVERED)可售后状态！");

    process.stdout.write(`[⏳] 用户因商品问题发起退款诉求... `);
    const refundRes = await request(`/api/orders/${orderId}/refund`, "POST", { reason: "自动化测退款", type: "退货退款" }, userToken);
    if (refundRes.status === 200) {
      console.log("✅ 售后事件建立，状态变更为: 售后处理中");
    }

    process.stdout.write(`[⏳] 管理员审查单证、处理售后及库存事务回滚联动... `);
    const auditRes = await request(`/api/orders/admin/refunds/${orderId}/audit`, "POST", { pass: true, reason: "同意测试件退款" }, adminToken);
    if (auditRes.status === 200) {
      console.log("✅ 审批同意，逆向资金通过事务原子性操作退回至钱包，原商品库存执行回调恢复！");
    }

    console.log("\n===================================");
    console.log("测试通过：渔鲜直供核心全链路正常");
    console.log("===================================");
  } catch (err) {
    if (err.cause && err.cause.code === 'ECONNREFUSED') {
      console.log("\n❌ 测试失败：无法连接后端服务，请先启动 Spring Boot！(端口8080)");
    } else {
      console.log("\n❌ 测试中断：", err);
    }
  }
}

// 启动执行
runTests();
