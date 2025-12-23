describe("优惠券营销完整流程测试", () => {
  beforeEach(() => {
    const user = {
      username: "testUser",
      password: "123456",
      displayName: "PromoUser",
    };
    cy.request({
      method: "POST",
      url: "/api/users/register",
      failOnStatusCode: false,
      body: user,
    });
    cy.login(user.username, user.password);
  });

  it("积分兑换优惠券并在下单时使用", () => {
    cy.window().then((win) => {
      const user = JSON.parse(win.localStorage.getItem("yuxian_user"));
      if (user) {
        user.points = 10000;
        win.localStorage.setItem("yuxian_user", JSON.stringify(user));
      }
    });

    cy.intercept("POST", "/api/coupons/exchange", {
      statusCode: 200,
      body: { success: true, points: 9500, message: "兑换成功" },
    }).as("exchangeReq");

    cy.visit("/points");
    cy.contains("会员积分中心");

    cy.contains("10000").should("exist");

    cy.contains("无门槛立减券")
      .parents(".group")
      .within(() => {
        cy.contains("兑换").click({ force: true });
      });

    cy.get(".swal2-confirm").click();

    cy.wait("@exchangeReq");
    cy.contains("兑换成功").should("be.visible");
    cy.get(".swal2-confirm").click();

    cy.visit("/");
    cy.contains("加载更多").should("exist");

    cy.get("main .group").first().find("button").last().click({ force: true });

    cy.contains("button", "购物车").should("contain", "1");

    cy.window().then((win) => {
      const user = JSON.parse(win.localStorage.getItem("yuxian_user"));
      if (user) {
        user.addresses = [
          {
            id: 999,
            contact: "测试收货人",
            phone: "13800138000",
            detail: "测试专用地址",
            tag: "公司",
            isDefault: true,
          },
        ];
        win.localStorage.setItem("yuxian_user", JSON.stringify(user));
      }
    });

    cy.intercept("GET", "/api/coupons/my*", [
      {
        id: 101,
        amount: 5,
        minSpend: 0,
        couponName: "无门槛立减券",
        status: "UNUSED",
        expiryDate: "2099-12-31",
      },
    ]).as("getMyCoupons");

    cy.intercept("POST", "/api/orders", {
      statusCode: 200,
      body: { orderId: "ORDER_123456", message: "下单成功" },
    }).as("createOrder");

    cy.intercept("POST", "/api/orders/*/pay", {
      statusCode: 200,
      body: { success: true },
    }).as("payOrder");

    cy.visit("/checkout");

    cy.contains("测试收货人").should("be.visible");

    cy.wait("@getMyCoupons");

    cy.get("select").should("contain", "无门槛立减券");
    cy.get("select").select("101");

    cy.contains("已成功抵扣 ¥5").should("be.visible");

    cy.contains("立即支付").click();

    cy.contains("支付成功", { timeout: 10000 }).should("be.visible");
  });
});
