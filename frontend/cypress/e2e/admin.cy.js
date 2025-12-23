describe('管理员后台测试', () => {
  const admin = { username: 'admin', password: '123', role: 'ADMIN' };
  
  const mockRefund = {
    id: 999,
    orderId: 8888,
    username: 'testUser',
    reason: '不喜欢，要求退款',
    status: 'PENDING',
    amount: 100.00,
    createTime: '2025-01-01T12:00:00',
    productNames: '帝王蟹',
    imageUrl: '/favicon.ico'
  };

  beforeEach(() => {

    cy.request({
      method: 'POST',
      url: '/api/users/register',
      failOnStatusCode: false,
      body: admin
    });
    
    cy.login(admin.username, admin.password);
  });

  it('管理员应能处理售后申请', () => {

    cy.intercept('GET', '/api/admin/stats', { totalSales: 9999, pendingOrders: 5 }).as('getStats');
    cy.intercept('GET', '/api/admin/orders*', { content: [], totalElements: 0 }).as('getOrders');
    
    cy.intercept('GET', '/api/orders/admin/refunds', [mockRefund]).as('getRefunds');

    cy.intercept('POST', '/api/orders/admin/refunds/*/audit', { 
      statusCode: 200, 
      body: { success: true } 
    }).as('auditRefund');

    cy.visit('/admin', {
      onBeforeLoad: (win) => {
        win.localStorage.setItem('role', 'ADMIN');

        win.localStorage.setItem('user', JSON.stringify({ ...admin, role: 'ADMIN' }));
      }
    });

    cy.url().should('include', '/admin');

    cy.wait(['@getStats', '@getOrders']);

    cy.contains('aside nav a', '售后处理').click();
    
    cy.wait('@getRefunds');

    cy.contains(mockRefund.reason).should('be.visible');
    cy.contains('待处理').should('be.visible');
    
    cy.contains('tr', mockRefund.username).within(() => {
      cy.contains('同意').click();
    });

    cy.get('.swal2-confirm').click();

    cy.wait('@auditRefund');
    cy.contains('已同意退款').should('be.visible');
  });
});