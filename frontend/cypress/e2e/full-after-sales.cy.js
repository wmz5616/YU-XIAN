describe("售后全流程闭环测试 (User -> Admin -> User)", () => {
  const TEST_ORDER_ID = 8888;
  const TEST_PRODUCT_NAME = "帝王蟹";
  const REFUND_REASON = "收到时冰袋化了，感觉不太新鲜";

  const user = {
    username: "refundUser",
    password: "123",
    displayName: "Refund Guy",
  };
  const admin = { username: "admin", password: "123", role: "ADMIN" };

  it("阶段一：用户登录并提交售后申请", () => {
    cy.request({
      method: "POST",
      url: "/api/users/register",
      failOnStatusCode: false,
      body: user,
    });
    cy.login(user.username, user.password);

    cy.intercept("GET", "/api/orders*", [
      {
        id: TEST_ORDER_ID,
        createTime: "2025-01-01T12:00:00",
        totalPrice: 1599,
        status: "DELIVERED",
        productNames: TEST_PRODUCT_NAME,
        items: [
          {
            id: 1,
            productName: TEST_PRODUCT_NAME,
            price: 1599,
            quantity: 1,
            imageUrl: "",
          },
        ],
      },
    ]).as("getOrders");

    cy.intercept("POST", `/api/orders/${TEST_ORDER_ID}/refund`, {
      statusCode: 200,
      body: { success: true },
    }).as("submitRefund");

    cy.visit("/orders");
    cy.wait("@getOrders");

    cy.contains(`#${TEST_ORDER_ID}`)
      .parents(".bg-white")
      .within(() => {
        cy.contains("申请售后").click();
      });

    cy.get("#swal-type").select("质量问题");
    cy.get("#swal-reason").type(REFUND_REASON);
    cy.get(".swal2-confirm").click();

    cy.wait("@submitRefund");
    cy.contains("已提交").should("be.visible");

    cy.clearLocalStorage();
  });

  it("阶段二：管理员登录并同意退款", () => {
    cy.login(admin.username, admin.password);
    cy.window().then((win) => win.localStorage.setItem("role", "ADMIN"));

    cy.intercept("GET", "/api/orders/admin/refunds", [
      {
        id: 101,
        orderId: TEST_ORDER_ID,
        productNames: TEST_PRODUCT_NAME,
        username: user.username,
        type: "质量问题",
        reason: REFUND_REASON,
        totalPrice: 1599,
        amount: 1599,
        status: "PENDING",
        createTime: "2025-01-02T10:00:00",
      },
    ]).as("getRefunds");

    cy.intercept("POST", "/api/orders/admin/refunds/*/audit", {
      statusCode: 200,
      body: { success: true },
    }).as("approveRefund");

    cy.intercept("GET", "/api/admin/stats", {
      totalSales: 0,
      pendingOrders: 0,
    }).as("getStats");
    cy.intercept("GET", "/api/admin/orders*", {
      content: [],
      totalElements: 0,
    }).as("getAdminOrders");

    cy.visit("/admin");
    cy.wait(["@getStats", "@getAdminOrders"]);

    cy.contains("aside nav a", "售后处理").click();
    cy.wait("@getRefunds");

    cy.contains(REFUND_REASON).should("be.visible");

    cy.contains("待处理").should("be.visible");

    cy.contains("tr", user.username).within(() => {
      cy.contains("同意").click();
    });

    cy.get(".swal2-confirm").click();

    cy.wait("@approveRefund");
    cy.contains("已同意退款").should("be.visible");

    cy.clearLocalStorage();
  });

  it("阶段三：用户登录并查收退款状态", () => {
    cy.login(user.username, user.password);

    cy.intercept("GET", "/api/products/orders*", [
      {
        id: TEST_ORDER_ID,
        totalPrice: 1599,
        status: "退款成功",
        productNames: TEST_PRODUCT_NAME,
        createTime: "2025-01-01T12:00:00",
        items: [
          {
            id: 1,
            productName: TEST_PRODUCT_NAME,
            price: 1599,
            quantity: 1,
            imageUrl: "",
          },
        ],
      },
    ]).as("getUserOrders");

    cy.intercept("GET", "/api/coupons/my*", []).as("getCoupons");

    cy.visit("/profile");

    cy.wait("@getUserOrders");

    cy.contains("售后服务").click();

    cy.contains("退款成功").should("be.visible");

    cy.get(".bg-green-500").should("exist");
  });
});
