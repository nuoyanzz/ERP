-- ============================================================
-- ERP 管理系统 初始化数据（MySQL 8.0+）
-- 说明：全部使用 INSERT ... ON DUPLICATE KEY UPDATE / INSERT IGNORE，可重复执行
-- ============================================================
USE erp;

-- ------------------------------------------------------------
-- 1. 菜单与权限点
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, path, component, icon, sort_no, visible) VALUES
(1000, 0, '系统管理', 1, NULL, '/sys', NULL, 'system', 1, 0),
(1001, 1000, '用户管理', 2, 'sys:user:query', '/sys/user', 'system/user/index', 'user', 1, 0),
(1002, 1000, '角色管理', 2, 'sys:role:query', '/sys/role', 'system/role/index', 'peoples', 2, 0),
(1003, 1000, '菜单管理', 2, 'sys:menu:query', '/sys/menu', 'system/menu/index', 'tree-table', 3, 0),
(1004, 1000, '操作日志', 2, 'sys:log:query', '/sys/log', 'sys/log/index', 'form', 4, 0),
(1005, 1000, '数据字典', 2, 'sys:dict:query', '/system/dict', 'sys/dict/index', 'dict', 5, 0),
(1006, 1000, '系统参数', 2, 'sys:param:query', '/sys/param', 'sys/param/index', 'setting', 6, 0),
(1007, 1000, '会计科目映射规则', 2, 'fi:rule:query', '/fi/rule', 'fi/rule/index', 'connection', 7, 0),
(1010, 1001, '用户新增', 3, 'sys:user:add', NULL, NULL, NULL, 1, 0),
(1011, 1001, '用户修改', 3, 'sys:user:edit', NULL, NULL, NULL, 2, 0),
(1012, 1001, '用户删除', 3, 'sys:user:remove', NULL, NULL, NULL, 3, 0),
(1013, 1001, '分配角色', 3, 'sys:user:assign', NULL, NULL, NULL, 4, 0),
(1014, 1001, '重置密码', 3, 'sys:user:resetPwd', NULL, NULL, NULL, 5, 0),
(1020, 1002, '角色新增', 3, 'sys:role:add', NULL, NULL, NULL, 1, 0),
(1021, 1002, '角色修改', 3, 'sys:role:edit', NULL, NULL, NULL, 2, 0),
(1022, 1002, '角色删除', 3, 'sys:role:remove', NULL, NULL, NULL, 3, 0),
(1023, 1002, '分配菜单', 3, 'sys:role:assign', NULL, NULL, NULL, 4, 0),
(1030, 1003, '菜单新增', 3, 'sys:menu:add', NULL, NULL, NULL, 1, 0),
(1031, 1003, '菜单修改', 3, 'sys:menu:edit', NULL, NULL, NULL, 2, 0),
(1032, 1003, '菜单删除', 3, 'sys:menu:remove', NULL, NULL, NULL, 3, 0),
(1050, 1004, '导出日志', 3, 'sys:log:export', NULL, NULL, NULL, 1, 0),
(1058, 1005, '字典新增', 3, 'sys:dict:add', NULL, NULL, NULL, 1, 0),
(1059, 1005, '字典修改', 3, 'sys:dict:edit', NULL, NULL, NULL, 2, 0),
(1060, 1005, '字典删除', 3, 'sys:dict:remove', NULL, NULL, NULL, 3, 0),
(1065, 1006, '参数新增', 3, 'sys:param:add', NULL, NULL, NULL, 1, 0),
(1066, 1006, '参数修改', 3, 'sys:param:edit', NULL, NULL, NULL, 2, 0),
(1067, 1006, '参数删除', 3, 'sys:param:remove', NULL, NULL, NULL, 3, 0),
(1055, 1007, '规则新增', 3, 'fi:rule:add', NULL, NULL, NULL, 1, 0),
(1056, 1007, '规则修改', 3, 'fi:rule:edit', NULL, NULL, NULL, 2, 0),
(1057, 1007, '规则删除', 3, 'fi:rule:remove', NULL, NULL, NULL, 3, 0),
(2000, 0, '主数据管理', 1, NULL, '/md', NULL, 'documentation', 2, 0),
(2001, 2000, '物料管理', 2, 'md:material:query', '/md/material', 'md/material/index', 'table', 1, 0),
(2002, 2000, '物料分类', 2, 'md:material-category:query', '/md/material-category', 'md/material-category/index', 'tree', 2, 0),
(2003, 2000, '客户管理', 2, 'md:customer:query', '/md/customer', 'md/customer/index', 'user', 3, 0),
(2004, 2000, '供应商管理', 2, 'md:supplier:query', '/md/supplier', 'md/supplier/index', 'peoples', 4, 0),
(2005, 2000, '仓库管理', 2, 'md:warehouse:query', '/md/warehouse', 'md/warehouse/index', 'house', 5, 0),
(2006, 2000, '计量单位', 2, 'md:unit:query', '/md/unit', 'md/unit/index', 'scale-to-original', 6, 0),
(2007, 2000, '价格表', 2, 'md:price:query', '/md/price', 'md/price/index', 'money', 7, 0),
(2013, 2001, '物料新增', 3, 'md:material:add', NULL, NULL, NULL, 1, 0),
(2014, 2001, '物料修改', 3, 'md:material:edit', NULL, NULL, NULL, 2, 0),
(2015, 2001, '物料删除', 3, 'md:material:remove', NULL, NULL, NULL, 3, 0),
(2020, 2002, '分类新增', 3, 'md:material-category:add', NULL, NULL, NULL, 1, 0),
(2021, 2002, '分类修改', 3, 'md:material-category:edit', NULL, NULL, NULL, 2, 0),
(2022, 2002, '分类删除', 3, 'md:material-category:remove', NULL, NULL, NULL, 3, 0),
(2030, 2003, '客户新增', 3, 'md:customer:add', NULL, NULL, NULL, 1, 0),
(2031, 2003, '客户修改', 3, 'md:customer:edit', NULL, NULL, NULL, 2, 0),
(2032, 2003, '客户删除', 3, 'md:customer:remove', NULL, NULL, NULL, 3, 0),
(2040, 2004, '供应商新增', 3, 'md:supplier:add', NULL, NULL, NULL, 1, 0),
(2041, 2004, '供应商修改', 3, 'md:supplier:edit', NULL, NULL, NULL, 2, 0),
(2042, 2004, '供应商删除', 3, 'md:supplier:remove', NULL, NULL, NULL, 3, 0),
(2050, 2005, '仓库新增', 3, 'md:warehouse:add', NULL, NULL, NULL, 1, 0),
(2051, 2005, '仓库修改', 3, 'md:warehouse:edit', NULL, NULL, NULL, 2, 0),
(2052, 2005, '仓库删除', 3, 'md:warehouse:remove', NULL, NULL, NULL, 3, 0),
(2060, 2006, '单位新增', 3, 'md:unit:add', NULL, NULL, NULL, 1, 0),
(2061, 2006, '单位修改', 3, 'md:unit:edit', NULL, NULL, NULL, 2, 0),
(2070, 2007, '价格新增', 3, 'md:price:add', NULL, NULL, NULL, 1, 0),
(2071, 2007, '价格修改', 3, 'md:price:edit', NULL, NULL, NULL, 2, 0),
(2072, 2007, '价格删除', 3, 'md:price:remove', NULL, NULL, NULL, 3, 0),
(3000, 0, '工作台', 2, NULL, '/dashboard', 'dashboard/index', 'dashboard', 0, 0),
(8000, 0, '财务管理', 1, NULL, '/fi', NULL, 'money', 3, 0),
(8001, 8000, '会计科目', 2, 'fi:account:query', '/fi/account', 'fi/account/index', 'tree-table', 1, 0),
(8002, 8000, '会计期间', 2, 'fi:period:query', '/fi/period', 'fi/period/index', 'date', 2, 0),
(8003, 8000, '记账凭证', 2, 'fi:voucher:query', '/fi/voucher', 'fi/voucher/index', 'documentation', 3, 0),
(8004, 8000, '科目余额表', 2, 'fi:balance:query', '/fi/balance', 'fi/balance/index', 'data-analysis', 4, 0),
(8005, 8000, '期末处理', 2, 'fi:carry:query', '/fi/carry-forward', 'fi/carry/index', 'setting', 5, 0),
(8006, 8000, '财务报表', 2, 'fi:report:query', '/fi/report', 'fi/report/index', 'chart', 6, 0),
(8007, 8000, '期初余额', 2, 'fi:opening:query', '/fi/opening', 'fi/opening/index', 'coin', 7, 0),
(8008, 8000, '试算平衡', 2, 'fi:trial:query', '/fi/trial', 'fi/trial/index', 'scale-to-original', 8, 0),
(8009, 8000, '汇率维护', 2, 'fi:rate:query', '/fi/rate', 'fi/rate/index', 'money', 9, 0),
(8010, 8001, '科目新增', 3, 'fi:account:add', NULL, NULL, NULL, 1, 0),
(8011, 8001, '科目修改', 3, 'fi:account:edit', NULL, NULL, NULL, 2, 0),
(8012, 8001, '科目删除', 3, 'fi:account:remove', NULL, NULL, NULL, 3, 0),
(8020, 8002, '期间新增', 3, 'fi:period:add', NULL, NULL, NULL, 1, 0),
(8021, 8002, '期间结账', 3, 'fi:period:close', NULL, NULL, NULL, 2, 0),
(8022, 8002, '期间反结账', 3, 'fi:period:open', NULL, NULL, NULL, 3, 0),
(8030, 8003, '凭证新增', 3, 'fi:voucher:add', NULL, NULL, NULL, 1, 0),
(8031, 8003, '凭证修改', 3, 'fi:voucher:edit', NULL, NULL, NULL, 2, 0),
(8032, 8003, '凭证删除', 3, 'fi:voucher:remove', NULL, NULL, NULL, 3, 0),
(8033, 8003, '凭证提交', 3, 'fi:voucher:submit', NULL, NULL, NULL, 4, 0),
(8034, 8003, '凭证审核', 3, 'fi:voucher:audit', NULL, NULL, NULL, 5, 0),
(8035, 8003, '凭证记账', 3, 'fi:voucher:post', NULL, NULL, NULL, 6, 0),
(8036, 8003, '红字冲销', 3, 'fi:voucher:redFlush', NULL, NULL, NULL, 7, 0),
(8037, 8003, '反记账', 3, 'fi:voucher:unpost', NULL, NULL, NULL, 8, 0),
(8038, 8003, '凭证作废', 3, 'fi:voucher:void', NULL, NULL, NULL, 9, 0),
(8039, 8003, '反审核', 3, 'fi:voucher:unaudit', NULL, NULL, NULL, 10, 0),
(8040, 8005, '损益结转', 3, 'fi:carry:execute', NULL, NULL, NULL, 1, 0),
(8041, 8005, '年末结转', 3, 'fi:carry:yearEnd', NULL, NULL, NULL, 2, 0),
(8042, 8005, '反结转', 3, 'fi:carry:reverse', NULL, NULL, NULL, 3, 0),
(8050, 8007, '期初余额导入', 3, 'fi:opening:import', NULL, NULL, NULL, 1, 0),
(8051, 8007, '期初余额录入', 3, 'fi:opening:add', NULL, NULL, NULL, 2, 0),
(8052, 8008, '生成试算平衡', 3, 'fi:trial:generate', NULL, NULL, NULL, 1, 0),
(8060, 8009, '汇率新增', 3, 'fi:rate:add', NULL, NULL, NULL, 1, 0),
(8061, 8009, '汇率修改', 3, 'fi:rate:edit', NULL, NULL, NULL, 2, 0),
(8062, 8009, '汇率删除', 3, 'fi:rate:remove', NULL, NULL, NULL, 3, 0),
(6000, 0, '采购管理', 1, NULL, '/pa', NULL, 'shopping', 4, 0),
(6001, 6000, '采购订单', 2, 'pa:order:query', '/pa/order', 'pa/order/index', 'document', 1, 0),
(6002, 6000, '采购入库单', 2, 'pa:instock:query', '/pa/instock', 'pa/instock/index', 'shopping-cart', 2, 0),
(6003, 6000, '采购退货单', 2, 'pa:return:query', '/pa/return', 'pa/return/index', 'refresh-left', 3, 0),
(6010, 6001, '订单新增', 3, 'pa:order:add', NULL, NULL, NULL, 1, 0),
(6011, 6001, '订单修改', 3, 'pa:order:edit', NULL, NULL, NULL, 2, 0),
(6012, 6001, '订单删除', 3, 'pa:order:remove', NULL, NULL, NULL, 3, 0),
(6013, 6001, '订单审核', 3, 'pa:order:audit', NULL, NULL, NULL, 4, 0),
(6014, 6001, '订单关闭', 3, 'pa:order:close', NULL, NULL, NULL, 5, 0),
(6015, 6001, '反审核', 3, 'pa:order:unaudit', NULL, NULL, NULL, 6, 0),
(6020, 6002, '入库单新增', 3, 'pa:instock:add', NULL, NULL, NULL, 1, 0),
(6021, 6002, '入库单修改', 3, 'pa:instock:edit', NULL, NULL, NULL, 2, 0),
(6022, 6002, '入库单删除', 3, 'pa:instock:remove', NULL, NULL, NULL, 3, 0),
(6023, 6002, '入库单审核', 3, 'pa:instock:audit', NULL, NULL, NULL, 4, 0),
(6024, 6002, '入库单反审核', 3, 'pa:instock:unaudit', NULL, NULL, NULL, 5, 0),
(6030, 6003, '退货单新增', 3, 'pa:return:add', NULL, NULL, NULL, 1, 0),
(6031, 6003, '退货单修改', 3, 'pa:return:edit', NULL, NULL, NULL, 2, 0),
(6032, 6003, '退货单删除', 3, 'pa:return:remove', NULL, NULL, NULL, 3, 0),
(6033, 6003, '退货单审核', 3, 'pa:return:audit', NULL, NULL, NULL, 4, 0),
(7000, 0, '库存管理', 1, NULL, '/ic', NULL, 'box', 5, 0),
(7001, 7000, '即时库存', 2, 'ic:stock:query', '/ic/stock', 'ic/stock/index', 'table', 1, 0),
(7002, 7000, '库存流水', 2, 'ic:transaction:query', '/ic/transaction', 'ic/transaction/index', 'list', 2, 0),
(7003, 7000, '库存调拨', 2, 'ic:transfer:query', '/ic/transfer', 'ic/transfer/index', 'swap', 3, 0),
(7004, 7000, '库存盘点', 2, 'ic:check:query', '/ic/check', 'ic/check/index', 'checkbox', 4, 0),
(7030, 7003, '调拨单新增', 3, 'ic:transfer:add', NULL, NULL, NULL, 1, 0),
(7031, 7003, '调拨单修改', 3, 'ic:transfer:edit', NULL, NULL, NULL, 2, 0),
(7032, 7003, '调拨单删除', 3, 'ic:transfer:remove', NULL, NULL, NULL, 3, 0),
(7033, 7003, '调拨单审核', 3, 'ic:transfer:audit', NULL, NULL, NULL, 4, 0),
(7040, 7004, '盘点单新增', 3, 'ic:check:add', NULL, NULL, NULL, 1, 0),
(7041, 7004, '盘点单修改', 3, 'ic:check:edit', NULL, NULL, NULL, 2, 0),
(7042, 7004, '盘点单删除', 3, 'ic:check:remove', NULL, NULL, NULL, 3, 0),
(7043, 7004, '盘点单审核', 3, 'ic:check:audit', NULL, NULL, NULL, 4, 0),
(5000, 0, '销售管理', 1, NULL, '/sa', NULL, 'sell', 4, 0),
(5001, 5000, '销售订单', 2, 'sa:order:query', '/sa/order', 'sa/order/index', 'document', 1, 0),
(5002, 5000, '销售出库单', 2, 'sa:outstock:query', '/sa/outstock', 'sa/outstock/index', 'sell', 2, 0),
(5003, 5000, '销售退货单', 2, 'sa:return:query', '/sa/return', 'sa/return/index', 'refresh-left', 3, 0),
(5010, 5001, '订单新增', 3, 'sa:order:add', NULL, NULL, NULL, 1, 0),
(5011, 5001, '订单修改', 3, 'sa:order:edit', NULL, NULL, NULL, 2, 0),
(5012, 5001, '订单删除', 3, 'sa:order:remove', NULL, NULL, NULL, 3, 0),
(5013, 5001, '订单审核', 3, 'sa:order:audit', NULL, NULL, NULL, 4, 0),
(5014, 5001, '订单关闭', 3, 'sa:order:close', NULL, NULL, NULL, 5, 0),
(5015, 5001, '反审核', 3, 'sa:order:unaudit', NULL, NULL, NULL, 6, 0),
(5020, 5002, '出库单新增', 3, 'sa:outstock:add', NULL, NULL, NULL, 1, 0),
(5021, 5002, '出库单修改', 3, 'sa:outstock:edit', NULL, NULL, NULL, 2, 0),
(5022, 5002, '出库单删除', 3, 'sa:outstock:remove', NULL, NULL, NULL, 3, 0),
(5023, 5002, '出库单审核', 3, 'sa:outstock:audit', NULL, NULL, NULL, 4, 0),
(5024, 5002, '出库单反审核', 3, 'sa:outstock:unaudit', NULL, NULL, NULL, 5, 0),
(5030, 5003, '退货单新增', 3, 'sa:return:add', NULL, NULL, NULL, 1, 0),
(5031, 5003, '退货单修改', 3, 'sa:return:edit', NULL, NULL, NULL, 2, 0),
(5032, 5003, '退货单删除', 3, 'sa:return:remove', NULL, NULL, NULL, 3, 0),
(5033, 5003, '退货单审核', 3, 'sa:return:audit', NULL, NULL, NULL, 4, 0),
(9000, 0, '流程管理', 1, NULL, '/wf', NULL, 'flow', 7, 0),
(9001, 9000, '流程定义', 2, 'wf:definition:query', '/wf/definition', 'wf/definition/index', 'tree-table', 1, 0),
(9002, 9000, '流程实例', 2, 'wf:instance:query', '/wf/instance', 'wf/instance/index', 'list', 2, 0),
(9003, 9000, '我的待办', 2, 'wf:task:todo', '/wf/task/todo', 'wf/todo/index', 'todo', 3, 0),
(9004, 9000, '我的已办', 2, 'wf:task:done', '/wf/task/done', 'wf/done/index', 'done', 4, 0),
(9010, 9001, '定义新增', 3, 'wf:definition:add', NULL, NULL, NULL, 1, 0),
(9011, 9001, '定义修改', 3, 'wf:definition:edit', NULL, NULL, NULL, 2, 0),
(9012, 9001, '定义删除', 3, 'wf:definition:remove', NULL, NULL, NULL, 3, 0),
(9013, 9001, '定义发布', 3, 'wf:definition:publish', NULL, NULL, NULL, 4, 0),
(9020, 9003, '审批同意', 3, 'wf:task:approve', NULL, NULL, NULL, 1, 0),
(9021, 9003, '审批驳回', 3, 'wf:task:reject', NULL, NULL, NULL, 2, 0),
(9022, 9003, '任务转办', 3, 'wf:task:transfer', NULL, NULL, NULL, 3, 0),
(9023, 9003, '任务加签', 3, 'wf:task:countersign', NULL, NULL, NULL, 4, 0),
(9024, 9003, '任务退回', 3, 'wf:task:rollback', NULL, NULL, NULL, 5, 0),
(9500, 0, '应收管理', 1, NULL, '/ar', NULL, 'money', 8, 0),
(9501, 9500, '应收单', 2, 'ar:receivable:query', '/ar/receivable', 'ar/receivable/index', 'documentation', 1, 0),
(9502, 9500, '收款单', 2, 'ar:receipt:query', '/ar/receipt', 'ar/receipt/index', 'money', 2, 0),
(9503, 9500, '账龄分析', 2, 'ar:aging:query', '/ar/receivable/aging', 'ar/aging/index', 'chart', 3, 0),
(9504, 9500, '坏账计提', 2, 'ar:baddebt:query', '/ar/baddebt', 'ar/baddebt/index', 'warning', 4, 0),
(9505, 9500, '往来对账', 2, 'ar:recon:query', '/ar/recon', 'ar/recon/index', 'connection', 5, 0),
(9510, 9502, '收款单新增', 3, 'ar:receipt:add', NULL, NULL, NULL, 1, 0),
(9511, 9502, '收款单审核', 3, 'ar:receipt:audit', NULL, NULL, NULL, 2, 0),
(9512, 9502, '收款单反审核', 3, 'ar:receipt:unaudit', NULL, NULL, NULL, 3, 0),
(9520, 9504, '坏账计提单新增', 3, 'ar:baddebt:add', NULL, NULL, NULL, 1, 0),
(9521, 9504, '坏账计提审核', 3, 'ar:baddebt:audit', NULL, NULL, NULL, 2, 0),
(9600, 0, '应付管理', 1, NULL, '/ap', NULL, 'money', 9, 0),
(9601, 9600, '应付单', 2, 'ap:payable:query', '/ap/payable', 'ap/payable/index', 'documentation', 1, 0),
(9602, 9600, '付款单', 2, 'ap:payment:query', '/ap/payment', 'ap/payment/index', 'money', 2, 0),
(9603, 9600, '账龄分析', 2, 'ap:aging:query', '/ap/aging', 'ap/aging/index', 'chart', 3, 0),
(9610, 9602, '付款单新增', 3, 'ap:payment:add', NULL, NULL, NULL, 1, 0),
(9611, 9602, '付款单审核', 3, 'ap:payment:audit', NULL, NULL, NULL, 2, 0),
(9612, 9602, '付款单反审核', 3, 'ap:payment:unaudit', NULL, NULL, NULL, 3, 0),
(9700, 0, '生产制造', 1, NULL, '/pm', NULL, 'industry', 10, 0),
(9701, 9700, 'BOM物料清单', 2, 'pm:bom:query', '/pm/bom', 'pm/bom/index', 'connection', 1, 0),
(9702, 9700, '生产订单', 2, 'pm:mo:query', '/pm/mo', 'pm/mo/index', 'documentation', 2, 0),
(9703, 9700, '生产领料单', 2, 'pm:pick:query', '/pm/pick', 'pm/pick/index', 'shopping-cart', 3, 0),
(9704, 9700, '完工入库单', 2, 'pm:finish:query', '/pm/finish', 'pm/finish/index', 'finished', 4, 0),
(9705, 9700, 'MRP运算', 2, 'pm:mrp:query', '/pm/mrp', 'pm/mrp/index', 'chart', 5, 0),
(9706, 9700, '工艺路线', 2, 'pm:routing:query', '/pm/routing', 'pm/routing/index', 'component', 6, 0),
(9707, 9700, '生产报工', 2, 'pm:report:query', '/pm/report', 'pm/report/index', 'edit', 7, 0),
(9708, 9700, '生产成本归集', 2, 'pm:cost:query', '/pm/cost', 'pm/cost/index', 'coin', 8, 0),
(9710, 9701, 'BOM新增', 3, 'pm:bom:add', NULL, NULL, NULL, 1, 0),
(9711, 9701, 'BOM修改', 3, 'pm:bom:edit', NULL, NULL, NULL, 2, 0),
(9712, 9701, 'BOM删除', 3, 'pm:bom:remove', NULL, NULL, NULL, 3, 0),
(9720, 9702, '订单新增', 3, 'pm:mo:add', NULL, NULL, NULL, 1, 0),
(9721, 9702, '订单修改', 3, 'pm:mo:edit', NULL, NULL, NULL, 2, 0),
(9722, 9702, '订单删除', 3, 'pm:mo:remove', NULL, NULL, NULL, 3, 0),
(9723, 9702, '订单审核', 3, 'pm:mo:audit', NULL, NULL, NULL, 4, 0),
(9724, 9702, '订单反审核', 3, 'pm:mo:unaudit', NULL, NULL, NULL, 5, 0),
(9730, 9703, '领料单新增', 3, 'pm:pick:add', NULL, NULL, NULL, 1, 0),
(9731, 9703, '领料单审核', 3, 'pm:pick:audit', NULL, NULL, NULL, 2, 0),
(9732, 9703, '领料单反审核', 3, 'pm:pick:unaudit', NULL, NULL, NULL, 3, 0),
(9740, 9704, '完工单新增', 3, 'pm:finish:add', NULL, NULL, NULL, 1, 0),
(9741, 9704, '完工单审核', 3, 'pm:finish:audit', NULL, NULL, NULL, 2, 0),
(9742, 9704, '完工单反审核', 3, 'pm:finish:unaudit', NULL, NULL, NULL, 3, 0),
(9750, 9705, '执行运算', 3, 'pm:mrp:run', NULL, NULL, NULL, 1, 0),
(9751, 9705, '投放采购订单', 3, 'pm:mrp:releasePurchase', NULL, NULL, NULL, 2, 0),
(9752, 9705, '投放生产订单', 3, 'pm:mrp:releaseProduction', NULL, NULL, NULL, 3, 0),
(9760, 9706, '工艺路线新增', 3, 'pm:routing:add', NULL, NULL, NULL, 1, 0),
(9761, 9706, '工艺路线修改', 3, 'pm:routing:edit', NULL, NULL, NULL, 2, 0),
(9762, 9706, '工艺路线删除', 3, 'pm:routing:remove', NULL, NULL, NULL, 3, 0),
(9770, 9707, '报工单新增', 3, 'pm:report:add', NULL, NULL, NULL, 1, 0),
(9771, 9707, '报工单审核', 3, 'pm:report:audit', NULL, NULL, NULL, 2, 0),
(9800, 0, '人力资源', 1, NULL, '/hr', NULL, 'people', 11, 0),
(9801, 9800, '组织架构', 2, 'hr:org:query', '/hr/org', 'hr/org/index', 'tree-table', 1, 0),
(9802, 9800, '部门管理', 2, 'hr:dept:query', '/hr/dept', 'hr/dept/index', 'tree-table', 2, 0),
(9803, 9800, '员工档案', 2, 'hr:employee:query', '/hr/employee', 'hr/employee/index', 'people', 3, 0),
(9804, 9800, '薪资核算', 2, 'hr:salary:query', '/hr/salary', 'hr/salary/index', 'money', 4, 0),
(9805, 9800, '考勤管理', 2, 'hr:attendance:query', '/hr/attendance', 'hr/attendance/index', 'date', 5, 0),
(9806, 9800, '社保个税政策', 2, 'hr:policy:query', '/hr/policy', 'hr/policy/index', 'setting', 6, 0),
(9810, 9801, '组织新增', 3, 'hr:org:add', NULL, NULL, NULL, 1, 0),
(9811, 9801, '组织修改', 3, 'hr:org:edit', NULL, NULL, NULL, 2, 0),
(9812, 9801, '组织删除', 3, 'hr:org:remove', NULL, NULL, NULL, 3, 0),
(9820, 9802, '部门新增', 3, 'hr:dept:add', NULL, NULL, NULL, 1, 0),
(9821, 9802, '部门修改', 3, 'hr:dept:edit', NULL, NULL, NULL, 2, 0),
(9822, 9802, '部门删除', 3, 'hr:dept:remove', NULL, NULL, NULL, 3, 0),
(9830, 9803, '员工新增', 3, 'hr:employee:add', NULL, NULL, NULL, 1, 0),
(9831, 9803, '员工修改', 3, 'hr:employee:edit', NULL, NULL, NULL, 2, 0),
(9832, 9803, '员工删除', 3, 'hr:employee:remove', NULL, NULL, NULL, 3, 0),
(9840, 9804, '薪资单新增', 3, 'hr:salary:add', NULL, NULL, NULL, 1, 0),
(9841, 9804, '薪资单审核', 3, 'hr:salary:audit', NULL, NULL, NULL, 2, 0),
(9850, 9805, '考勤新增', 3, 'hr:attendance:add', NULL, NULL, NULL, 1, 0),
(9851, 9805, '考勤修改', 3, 'hr:attendance:edit', NULL, NULL, NULL, 2, 0),
(9852, 9805, '考勤删除', 3, 'hr:attendance:remove', NULL, NULL, NULL, 3, 0),
(9860, 9806, '政策新增', 3, 'hr:policy:add', NULL, NULL, NULL, 1, 0),
(9861, 9806, '政策修改', 3, 'hr:policy:edit', NULL, NULL, NULL, 2, 0),
(9862, 9806, '政策删除', 3, 'hr:policy:remove', NULL, NULL, NULL, 3, 0),
(9613, 9602, '付款单修改', 3, 'ap:payment:edit', NULL, NULL, NULL, 3, 0),
(9614, 9602, '付款单删除', 3, 'ap:payment:remove', NULL, NULL, NULL, 4, 0),
(9513, 9502, '收款单修改', 3, 'ar:receipt:edit', NULL, NULL, NULL, 3, 0),
(9514, 9502, '收款单删除', 3, 'ar:receipt:remove', NULL, NULL, NULL, 4, 0),
(9842, 9804, '薪资单修改', 3, 'hr:salary:edit', NULL, NULL, NULL, 2, 0),
(9843, 9804, '薪资单删除', 3, 'hr:salary:remove', NULL, NULL, NULL, 3, 0),
(9743, 9704, '完工单修改', 3, 'pm:finish:edit', NULL, NULL, NULL, 2, 0),
(9744, 9704, '完工单删除', 3, 'pm:finish:remove', NULL, NULL, NULL, 3, 0),
(9733, 9703, '领料单修改', 3, 'pm:pick:edit', NULL, NULL, NULL, 2, 0),
(9734, 9703, '领料单删除', 3, 'pm:pick:remove', NULL, NULL, NULL, 3, 0),
(9025, 9002, '发起流程', 3, 'wf:instance:add', NULL, NULL, NULL, 1, 0),
(9026, 9003, '任务查询', 3, 'wf:task:query', NULL, NULL, NULL, 6, 0)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms), path = VALUES(path),
    component = VALUES(component), icon = VALUES(icon), sort_no = VALUES(sort_no), visible = VALUES(visible);

-- ------------------------------------------------------------
-- 2. 角色
-- ------------------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, data_scope, status, remark) VALUES
(1, 'admin', '超级管理员', 1, 0, '系统内置超级管理员，拥有全部权限'),
(2, 'finance', '财务主管', 1, 0, '负责记账、结账与报表，数据范围为全部'),
(3, 'purchaser', '采购员', 2, 0, '负责采购业务，数据范围为本部门'),
(4, 'seller', '销售员', 2, 0, '负责销售业务，数据范围为本部门'),
(5, 'warehouse', '仓管员', 3, 0, '负责出入库与盘点，数据范围为本部门及以下'),
(6, 'hr', '人事专员', 1, 0, '负责人事与薪资核算'),
(7, 'producer', '生产计划员', 3, 0, '负责生产与MRP，数据范围为本部门及以下')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), data_scope = VALUES(data_scope),
    status = VALUES(status), remark = VALUES(remark);

-- 管理员拥有全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 财务主管：财务模块 + 系统查看 + 应收应付 + 待办
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (
  SELECT id FROM sys_menu WHERE id BETWEEN 8000 AND 8069
  UNION SELECT id FROM sys_menu WHERE id BETWEEN 9500 AND 9529
  UNION SELECT id FROM sys_menu WHERE id BETWEEN 9600 AND 9619
  UNION SELECT id FROM sys_menu WHERE (id IN (1000,1001,1002,1003,1004,3000))
  UNION SELECT id FROM sys_menu WHERE id IN (9002,9003,9004,9020,9021,9022,9023,9024,9026)
);

-- 采购员：采购 + 主数据查询 + 库存查询 + 待办
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE id IN (
  SELECT id FROM sys_menu WHERE id BETWEEN 6000 AND 6039
  UNION SELECT id FROM sys_menu WHERE id IN (2000,2001,2002,2004,2005,7001,7002,3000)
  UNION SELECT id FROM sys_menu WHERE id IN (9003,9004,9020,9021,9022,9023,9024,9026)
);

-- 销售员：销售 + 客户 + 库存查询 + 应收 + 待办
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 4, id FROM sys_menu WHERE id IN (
  SELECT id FROM sys_menu WHERE id BETWEEN 5000 AND 5039
  UNION SELECT id FROM sys_menu WHERE id IN (2000,2001,2002,2003,7001,7002,9500,9501,9502,9503,3000)
  UNION SELECT id FROM sys_menu WHERE id IN (9003,9004,9020,9021,9022,9023,9024,9026)
);

-- 仓管员：库存全部 + 采购销售查询 + 收发货 + 待办
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 5, id FROM sys_menu WHERE id IN (
  SELECT id FROM sys_menu WHERE id BETWEEN 7000 AND 7049
  UNION SELECT id FROM sys_menu WHERE id IN (2000,2001,2002,2005,6001,6002,5001,5002,3000)
  UNION SELECT id FROM sys_menu WHERE id IN (9003,9004,9020,9021,9022,9023,9024,9026)
);

-- 人事专员：人力模块 + 待办
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 6, id FROM sys_menu WHERE id IN (
  SELECT id FROM sys_menu WHERE id BETWEEN 9800 AND 9869
  UNION SELECT id FROM sys_menu WHERE id IN (1000,1001,3000)
  UNION SELECT id FROM sys_menu WHERE id IN (9003,9004,9020,9021,9022,9023,9024,9026)
);

-- 生产计划员：生产全部 + BOM + 物料 + 库存 + 待办
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 7, id FROM sys_menu WHERE id IN (
  SELECT id FROM sys_menu WHERE id BETWEEN 9700 AND 9779
  UNION SELECT id FROM sys_menu WHERE id IN (2000,2001,2002,2005,7001,7002,6001,3000)
  UNION SELECT id FROM sys_menu WHERE id IN (9003,9004,9020,9021,9022,9023,9024,9026)
);

-- ------------------------------------------------------------
-- 3. 演示账号
--    演示账号使用统一口令，公开仓库中不应内置。
--    需要跑通演示流程时，执行 seed/demo_accounts.sql 单独导入。
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 4. 组织 / 部门 / 员工
-- ------------------------------------------------------------
INSERT INTO hr_org (id, org_code, org_name, org_type, parent_id, remark) VALUES
(96001, 'ORG-001', '示例科技有限公司', 1, 0, '总部')
ON DUPLICATE KEY UPDATE org_name = VALUES(org_name), org_type = VALUES(org_type), remark = VALUES(remark);

INSERT INTO hr_dept (id, dept_code, dept_name, org_id, parent_id, remark) VALUES
(96011, 'DEPT-001', '总经办', 96001, 0, '公司管理部门'),
(96012, 'DEPT-002', '财务部', 96001, 0, NULL),
(96013, 'DEPT-003', '采购部', 96001, 0, NULL),
(96014, 'DEPT-004', '销售部', 96001, 0, NULL),
(96015, 'DEPT-005', '生产部', 96001, 0, NULL),
(96016, 'DEPT-006', '人力资源部', 96001, 0, NULL),
(96017, 'DEPT-007', '仓储部', 96001, 0, NULL)
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name), org_id = VALUES(org_id),
    parent_id = VALUES(parent_id), remark = VALUES(remark);

INSERT INTO hr_employee (id, employee_no, employee_name, gender, dept_id, position, entry_date, phone, status, base_salary, social_base, fund_base) VALUES
(97001, 'EMP-001', '张明', 1, 96011, '总经理', '2020-01-06', '13800000001', 0, 30000, 30000, 30000),
(97002, 'EMP-002', '李芳', 2, 96012, '财务经理', '2020-03-16', '13800000002', 0, 18000, 18000, 18000),
(97003, 'EMP-003', '王强', 1, 96013, '采购主管', '2021-05-10', '13800000003', 0, 12000, 12000, 12000),
(97004, 'EMP-004', '赵敏', 2, 96014, '销售经理', '2021-07-19', '13800000004', 0, 12000, 12000, 12000),
(97005, 'EMP-005', '孙磊', 1, 96015, '生产主管', '2021-09-01', '13800000005', 0, 11000, 11000, 11000),
(97006, 'EMP-006', '周静', 2, 96017, '仓管员', '2022-02-14', '13800000006', 0, 7000, 7000, 7000),
(97007, 'EMP-007', '吴刚', 1, 96016, '人事专员', '2022-04-01', '13800000007', 0, 8000, 8000, 8000)
ON DUPLICATE KEY UPDATE employee_name = VALUES(employee_name), gender = VALUES(gender),
    dept_id = VALUES(dept_id), position = VALUES(position), status = VALUES(status),
    base_salary = VALUES(base_salary), social_base = VALUES(social_base), fund_base = VALUES(fund_base);

-- ------------------------------------------------------------
-- 4. 主数据：物料分类 / 计量单位 / 物料 / 仓库 / 客户 / 供应商
-- ------------------------------------------------------------
INSERT INTO md_material_category (id, category_code, category_name, parent_id, sort_no) VALUES
(4000, 'RAW', '原材料', 0, 1),
(4001, 'RAW-METAL', '金属材料', 4000, 1),
(4002, 'RAW-PLASTIC', '塑胶材料', 4000, 2),
(4003, 'RAW-ELEC', '电子元器件', 4000, 3),
(4100, 'PRODUCT', '成品', 0, 2),
(4101, 'PRODUCT-A', '成品A', 4100, 1),
(4200, 'SEMI', '半成品', 0, 3),
(4300, 'CONS', '低值易耗品', 0, 4)
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name), parent_id = VALUES(parent_id), sort_no = VALUES(sort_no);

INSERT INTO md_unit (id, unit_code, unit_name, remark) VALUES
(43001, 'PCS', '个', '基本计量单位'),
(43002, 'KG', '千克', NULL),
(43003, 'M', '米', NULL),
(43004, 'TON', '吨', NULL),
(43005, 'BOX', '箱', NULL),
(43006, 'SET', '套', NULL)
ON DUPLICATE KEY UPDATE unit_name = VALUES(unit_name), remark = VALUES(remark);

INSERT INTO md_material (id, material_code, material_name, spec, category_id, unit, track_batch, track_sn, cost_price, safe_stock, status, procure_type, lead_time_days, lot_size, min_order_qty, tax_rate, default_supplier_id, remark) VALUES
(44001, 'RM-STEEL-01', '冷轧钢板', 'SPCC 1.0mm 1250x2500', 4001, 'KG', 0, 0, 12.5000, 500, 0, 1, 7, 100, 50, 13, 7000, '主要原材料'),
(44002, 'RM-PLASTIC-01', 'ABS工程塑料', '牌号 750A 本色', 4002, 'KG', 0, 0, 18.8000, 300, 0, 1, 10, 50, 50, 13, 7000, NULL),
(44003, 'RM-ELEC-01', '贴片电阻 0603', '1% 10KΩ', 4003, 'PCS', 1, 1, 0.0250, 10000, 0, 1, 15, 5000, 1000, 13, 7000, '批次+序列号管理'),
(44004, 'RM-ELEC-02', '贴片电容 0805', 'X7R 100nF', 4003, 'PCS', 1, 0, 0.0180, 20000, 0, 1, 15, 5000, 1000, 13, 7000, NULL),
(44005, 'SF-BOX-01', '包装纸箱', '400x300x200mm 五层瓦楞', 4300, 'PCS', 0, 0, 3.2000, 1000, 0, 1, 5, 200, 100, 13, 7001, NULL),
(44006, 'SF-BAG-01', '防静电袋', '150x200mm', 4300, 'PCS', 0, 0, 0.3500, 5000, 0, 1, 5, 500, 500, 13, 7001, NULL),
(45001, 'SF-PANEL-A', '控制面板总成', 'A型 220V', 4101, 'SET', 1, 1, 386.0000, 50, 0, 2, 3, 0, 0, 13, NULL, '成品，自制'),
(45002, 'SF-MOTOR-B', '驱动电机总成', 'B型 750W', 4101, 'SET', 1, 0, 245.0000, 80, 0, 2, 3, 0, 0, 13, NULL, '成品，自制'),
(46001, 'SF-CABLE-A', '连接线束', '1.2m 24AWG', 4200, 'PCS', 0, 0, 18.5000, 200, 0, 2, 2, 0, 0, 13, NULL, '半成品，自制')
ON DUPLICATE KEY UPDATE material_name = VALUES(material_name), spec = VALUES(spec),
    category_id = VALUES(category_id), unit = VALUES(unit), track_batch = VALUES(track_batch),
    track_sn = VALUES(track_sn), cost_price = VALUES(cost_price), safe_stock = VALUES(safe_stock),
    status = VALUES(status), procure_type = VALUES(procure_type), lead_time_days = VALUES(lead_time_days),
    lot_size = VALUES(lot_size), min_order_qty = VALUES(min_order_qty), tax_rate = VALUES(tax_rate),
    default_supplier_id = VALUES(default_supplier_id), remark = VALUES(remark);

INSERT INTO md_warehouse (id, warehouse_code, warehouse_name, wh_type, location, allow_negative, freeze_stock) VALUES
(5000, 'RAW001', '原料仓', 1, '一号厂房', 0, 0),
(5001, 'FG001', '成品仓', 1, '成品库房', 0, 0),
(5002, 'NG001', '不良品仓', 2, '待处理区', 0, 0),
(5003, 'WIP001', '在制品仓', 3, '生产线', 0, 0),
(5004, 'QC001', '待检仓', 4, '质检区', 0, 0)
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name), wh_type = VALUES(wh_type),
    location = VALUES(location), allow_negative = VALUES(allow_negative), freeze_stock = VALUES(freeze_stock);

INSERT INTO md_customer (id, customer_code, customer_name, credit_limit, currency, tax_no, contact_name, contact_phone, sales_dept_id, payment_days, discount_rate, bank_name, bank_account, status) VALUES
(6000, 'C0001', '示例客户A', 100000, 'CNY', '91310000MA1K0000X', '张三', '13800000001', 96014, 30, 100, '工商银行上海分行', '6222021234567890123', 0),
(6001, 'C0002', '示例客户B', 50000, 'CNY', '91310000MA1K0000Y', '李四', '13800000002', 96014, 45, 98, '建设银行上海分行', '6222029876543210987', 0),
(6002, 'C0003', '示例客户C', 200000, 'CNY', '91310000MA1K0000Z', '王五', '13800000003', 96014, 60, 95, '招商银行上海分行', '6222025555666677889', 0),
(6003, 'C0004', '示例客户D', 30000, 'CNY', '91310000MA1K0001A', '赵六', '13800000004', 96014, 15, 100, NULL, NULL, 1)
ON DUPLICATE KEY UPDATE customer_name = VALUES(customer_name), credit_limit = VALUES(credit_limit),
    currency = VALUES(currency), tax_no = VALUES(tax_no), contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone), sales_dept_id = VALUES(sales_dept_id),
    payment_days = VALUES(payment_days), discount_rate = VALUES(discount_rate),
    bank_name = VALUES(bank_name), bank_account = VALUES(bank_account), status = VALUES(status);

INSERT INTO md_supplier (id, supplier_code, supplier_name, tax_no, settle_type, level, contact_name, contact_phone, payment_days, bank_name, bank_account, status) VALUES
(7000, 'S0001', '示例供应商A', '91310000MA1K0 example', '月结30天', 1, '王五', '13900000001', 30, '工商银行上海分行', '6222021111222233333', 0),
(7001, 'S0002', '示例供应商B', '91310000MA1K0 sample', '货到付款', 2, '赵六', '13900000002', 0, '建设银行上海分行', '6222024444555566666', 0),
(7002, 'S0003', '示例供应商C', '91310000MA1K0 demo', '月结60天', 1, '钱七', '13900000003', 60, NULL, NULL, 0)
ON DUPLICATE KEY UPDATE supplier_name = VALUES(supplier_name), tax_no = VALUES(tax_no),
    settle_type = VALUES(settle_type), level = VALUES(level), contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone), payment_days = VALUES(payment_days),
    bank_name = VALUES(bank_name), bank_account = VALUES(bank_account), status = VALUES(status);

-- ------------------------------------------------------------
-- 5. 会计期间
-- ------------------------------------------------------------
INSERT INTO fi_period (id, period_year, period_no, period_month, status, is_year_end) VALUES
(900101, 2026, 1, '2026-01', 1, 0),
(900102, 2026, 2, '2026-02', 1, 0),
(900103, 2026, 3, '2026-03', 1, 0),
(900104, 2026, 4, '2026-04', 1, 0),
(900105, 2026, 5, '2026-05', 1, 0),
(900106, 2026, 6, '2026-06', 1, 0),
(900107, 2026, 7, '2026-07', 1, 0),
(900108, 2026, 8, '2026-08', 1, 0),
(900109, 2026, 9, '2026-09', 1, 0),
(900110, 2026, 10, '2026-10', 1, 0),
(900111, 2026, 11, '2026-11', 1, 0),
(900112, 2026, 12, '2026-12', 1, 1)
ON DUPLICATE KEY UPDATE period_year = VALUES(period_year), period_no = VALUES(period_no),
    status = VALUES(status), is_year_end = VALUES(is_year_end);

-- ------------------------------------------------------------
-- 6. 会计科目（按企业会计准则）
-- ------------------------------------------------------------
INSERT INTO fi_account (id, subject_code, subject_name, parent_id, category, balance_dir, status, is_leaf, carry_forward, cash_flow_type, quantity_account, unit) VALUES
(91001, '1001', '库存现金', 0, 1, 1, 0, 1, 1, 1, 0, NULL),
(91002, '1002', '银行存款', 0, 1, 1, 0, 0, 1, 1, 0, NULL),
(91003, '100201', '银行存款-工商银行', 91002, 1, 1, 0, 1, 1, 1, 0, NULL),
(91004, '100202', '银行存款-建设银行', 91002, 1, 1, 0, 1, 1, 1, 0, NULL),
(91005, '1122', '应收账款', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91006, '1221', '其他应收款', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91007, '1403', '原材料', 0, 1, 1, 0, 1, 1, 0, 1, NULL),
(91008, '1405', '库存商品', 0, 1, 1, 0, 1, 1, 0, 1, NULL),
(91009, '1408', '委托加工物资', 0, 1, 1, 0, 1, 1, 0, 1, NULL),
(91010, '1471', '存货跌价准备', 0, 1, 2, 0, 1, 1, 0, 0, NULL),
(91011, '1601', '固定资产', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91012, '1602', '累计折旧', 0, 1, 2, 0, 1, 1, 0, 0, NULL),
(91013, '1606', '固定资产清理', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91014, '1701', '无形资产', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91015, '1801', '长期待摊费用', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91016, '1901', '待处理财产损溢', 0, 1, 1, 0, 1, 1, 0, 0, NULL),
(91017, '2001', '短期借款', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
(91018, '2202', '应付账款', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
(91019, '2203', '预付账款', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
(91020, '2211', '应付职工薪酬', 0, 2, 2, 0, 0, 1, 0, 0, NULL),
(91021, '221101', '应付职工薪酬-工资', 91020, 2, 2, 0, 1, 1, 0, 0, NULL),
(91022, '221102', '应付职工薪酬-社会保险费', 91020, 2, 2, 0, 1, 1, 0, 0, NULL),
(91023, '221103', '应付职工薪酬-住房公积金', 91020, 2, 2, 0, 1, 1, 0, 0, NULL),
(91024, '221104', '应付职工薪酬-职工福利费', 91020, 2, 2, 0, 1, 1, 0, 0, NULL),
(91025, '221105', '应付职工薪酬-工会经费', 91020, 2, 2, 0, 1, 1, 0, 0, NULL),
(91026, '2221', '应交税费', 0, 2, 2, 0, 0, 1, 0, 0, NULL),
(91027, '222101', '应交税费-应交增值税', 91026, 2, 2, 0, 0, 1, 0, 0, NULL),
(91028, '22210101', '应交税费-应交增值税(进项税额)', 91027, 2, 2, 0, 1, 1, 0, 0, NULL),
(91029, '22210102', '应交税费-应交增值税(销项税额)', 91027, 2, 2, 0, 1, 1, 0, 0, NULL),
(91030, '222102', '应交税费-应交企业所得税', 91026, 2, 2, 0, 1, 1, 0, 0, NULL),
(91031, '222103', '应交税费-应交个人所得税', 91026, 2, 2, 0, 1, 1, 0, 0, NULL),
(91032, '2241', '其他应付款', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
(91033, '2241.01', '其他应付款-代扣社保', 91032, 2, 2, 0, 1, 1, 0, 0, NULL),
(91034, '2241.02', '其他应付款-代扣个税', 91032, 2, 2, 0, 1, 1, 0, 0, NULL),
(91035, '2401', '预计负债', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
(91036, '2501', '长期借款', 0, 2, 2, 0, 1, 1, 0, 0, NULL),
(91037, '4001', '主营业务收入', 0, 5, 2, 0, 1, 1, 0, 0, NULL),
(91038, '4002', '主营业务成本', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91039, '4003', '税金及附加', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91040, '4103', '本年利润', 0, 3, 2, 0, 1, 0, 0, 0, NULL),
(91041, '4104', '利润分配', 0, 3, 2, 0, 0, 0, 0, 0, NULL),
(91042, '410401', '利润分配-未分配利润', 91041, 3, 2, 0, 1, 0, 0, 0, NULL),
(91043, '5001', '生产成本', 0, 4, 1, 0, 0, 0, 0, 0, NULL),
(91044, '500101', '生产成本-基本生产成本', 91043, 4, 1, 0, 1, 0, 0, 0, NULL),
(91045, '500102', '生产成本-辅助生产成本', 91043, 4, 1, 0, 1, 0, 0, 0, NULL),
(91046, '5101', '制造费用', 0, 4, 1, 0, 1, 0, 0, 0, NULL),
(91047, '6001', '营业收入', 0, 5, 2, 0, 1, 1, 0, 0, NULL),
(91048, '6051', '其他业务收入', 0, 5, 2, 0, 1, 1, 0, 0, NULL),
(91049, '6111', '投资收益', 0, 5, 2, 0, 1, 1, 0, 0, NULL),
(91050, '6401', '主营业务收入(旧)', 0, 5, 2, 0, 1, 1, 0, 0, NULL),
(91051, '6402', '主营业务成本(旧)', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91052, '6601', '销售费用', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91053, '6602', '管理费用', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91054, '6603', '财务费用', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91055, '6604', '税金及附加(旧)', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91056, '6801', '所得税费用', 0, 5, 1, 0, 1, 1, 0, 0, NULL),
(91057, '6901', '以前年度损益调整', 0, 5, 2, 0, 1, 0, 0, 0, NULL)
ON DUPLICATE KEY UPDATE subject_name = VALUES(subject_name), parent_id = VALUES(parent_id),
    category = VALUES(category), balance_dir = VALUES(balance_dir), status = VALUES(status),
    is_leaf = VALUES(is_leaf), carry_forward = VALUES(carry_forward),
    cash_flow_type = VALUES(cash_flow_type), quantity_account = VALUES(quantity_account), unit = VALUES(unit);

UPDATE fi_account SET assist_customer = 1 WHERE subject_code IN ('1122');
UPDATE fi_account SET assist_supplier = 1 WHERE subject_code IN ('2202');
UPDATE fi_account SET assist_dept = 1 WHERE subject_code IN ('6601','6602','6603');
UPDATE fi_account SET cash_flow_type = 2 WHERE subject_code IN ('1001','1002','100201','100202');

-- ------------------------------------------------------------
-- 7. 业务单据 → 会计科目映射规则（借贷双科目 + 金额来源）
-- ------------------------------------------------------------
INSERT INTO fi_biz_account_rule (id, biz_type, rule_key, subject_code, debit_subject_code, credit_subject_code, amount_source, cash_flow_type, remark) VALUES
(95001, 'PA_INSTOCK', 'MATERIAL', '1403', '1403', NULL, 'COST', 0, '采购入库-借方：原材料（不含税）'),
(95002, 'PA_INSTOCK', 'TAX', '22210101', '22210101', NULL, 'TAX', 0, '采购入库-借方：进项税额'),
(95003, 'PA_INSTOCK', 'PAYABLE', '2202', NULL, '2202', 'TOTAL', 0, '采购入库-贷方：应付账款（价税合计）'),
(95004, 'SA_OUTSTOCK', 'REVENUE', '4001', NULL, '4001', 'NET_AMOUNT', 0, '销售出库-贷方：主营业务收入（不含税）'),
(95005, 'SA_OUTSTOCK', 'TAX', '22210102', NULL, '22210102', 'TAX', 0, '销售出库-贷方：销项税额'),
(95006, 'SA_OUTSTOCK', 'RECEIVABLE', '1122', '1122', NULL, 'TOTAL', 0, '销售出库-借方：应收账款（价税合计）'),
(95007, 'SA_OUTSTOCK', 'COST', '4002', '4002', NULL, 'COST', 0, '销售出库-借方：主营业务成本'),
(95008, 'SA_OUTSTOCK', 'STOCK', '1405', NULL, '1405', 'COST', 0, '销售出库-贷方：库存商品'),
(95009, 'IC_CHECK', 'STOCK', '1405', '1405', NULL, 'AMOUNT', 0, '盘点-借方：库存商品'),
(95010, 'IC_CHECK', 'SUSPENSE', '1901', NULL, '1901', 'AMOUNT', 0, '盘点-贷方：待处理财产损溢'),
(95011, 'PA_RETURN', 'MATERIAL', '1403', NULL, '1403', 'COST', 0, '采购退货-贷方：原材料（红字）'),
(95012, 'PA_RETURN', 'TAX', '22210101', NULL, '22210101', 'TAX', 0, '采购退货-贷方：进项税额转出（红字）'),
(95013, 'PA_RETURN', 'PAYABLE', '2202', '2202', NULL, 'TOTAL', 0, '采购退货-借方：应付账款（红字）'),
(95014, 'SA_RETURN', 'REVENUE', '4001', '4001', NULL, 'NET_AMOUNT', 0, '销售退货-借方：主营业务收入（红字）'),
(95015, 'SA_RETURN', 'TAX', '22210102', '22210102', NULL, 'TAX', 0, '销售退货-借方：销项税额转出（红字）'),
(95016, 'SA_RETURN', 'RECEIVABLE', '1122', NULL, '1122', 'TOTAL', 0, '销售退货-贷方：应收账款（红字）'),
(95017, 'SA_RETURN', 'COST', '4002', NULL, '4002', 'COST', 0, '销售退货-贷方：主营业务成本（红字）'),
(95018, 'SA_RETURN', 'STOCK', '1405', '1405', NULL, 'COST', 0, '销售退货-借方：库存商品（红字）'),
(95019, 'AR_RECEIPT', 'BANK', '1002', '1002', NULL, 'TOTAL', 1, '收款-借方：银行存款'),
(95020, 'AR_RECEIPT', 'RECEIVABLE', '1122', NULL, '1122', 'TOTAL', 0, '收款-贷方：应收账款'),
(95021, 'AP_PAYMENT', 'PAYABLE', '2202', '2202', NULL, 'TOTAL', 0, '付款-借方：应付账款'),
(95022, 'AP_PAYMENT', 'BANK', '1002', NULL, '1002', 'TOTAL', 1, '付款-贷方：银行存款'),
(95023, 'PM_PICK', 'COST', '5001', '5001', NULL, 'COST', 0, '生产领料-借方：生产成本'),
(95024, 'PM_PICK', 'MATERIAL', '1403', NULL, '1403', 'COST', 0, '生产领料-贷方：原材料'),
(95025, 'PM_FINISH', 'STOCK', '1405', '1405', NULL, 'COST', 0, '完工入库-借方：库存商品'),
(95026, 'PM_FINISH', 'COST', '5001', NULL, '5001', 'COST', 0, '完工入库-贷方：生产成本'),
(95027, 'HR_SALARY', 'EXPENSE', '6602', '6602', NULL, 'GROSS', 0, '薪资-借方：管理费用（应发总额）'),
(95028, 'HR_SALARY', 'PAYABLE', '221101', NULL, '221101', 'GROSS', 0, '薪资-贷方：应付职工薪酬-工资'),
(95029, 'HR_SALARY', 'SOCIAL', '221102', NULL, '221102', 'SOCIAL_PERSONAL', 0, '薪资-贷方：应付职工薪酬-社会保险费（个人）'),
(95030, 'HR_SALARY', 'TAX', '222103', NULL, '222103', 'TAX', 0, '薪资-贷方：应交税费-应交个人所得税'),
(95031, 'CARRY_FORWARD', 'PROFIT', '4103', NULL, '4103', 'AMOUNT', 0, '损益结转-贷方：本年利润'),
(95032, 'CARRY_FORWARD', 'LOSS', '4103', '4103', NULL, 'AMOUNT', 0, '损益结转-借方：本年利润'),
(95033, 'YEAR_END', 'PROFIT', '410401', NULL, '410401', 'AMOUNT', 0, '年末结转-贷方：未分配利润'),
(95034, 'YEAR_END', 'LOSS', '410401', '410401', NULL, 'AMOUNT', 0, '年末结转-借方：未分配利润'),
(95035, 'YEAR_END', 'SURPLUS', '4103', '4103', NULL, 'AMOUNT', 0, '年末结转-借方：本年利润')
ON DUPLICATE KEY UPDATE subject_code = VALUES(subject_code), debit_subject_code = VALUES(debit_subject_code),
    credit_subject_code = VALUES(credit_subject_code), amount_source = VALUES(amount_source),
    cash_flow_type = VALUES(cash_flow_type), remark = VALUES(remark);

-- ------------------------------------------------------------
-- 8. 汇率
-- ------------------------------------------------------------
INSERT INTO fi_exchange_rate (id, currency, period, rate, rate_type) VALUES
(98001, 'USD', '2026-01', 7.100000, 1),
(98002, 'USD', '2026-02', 7.150000, 1),
(98003, 'USD', '2026-03', 7.200000, 1),
(98004, 'HKD', '2026-01', 0.910000, 1),
(98005, 'HKD', '2026-02', 0.905000, 1),
(98006, 'EUR', '2026-01', 7.650000, 1),
(98007, 'EUR', '2026-02', 7.600000, 1)
ON DUPLICATE KEY UPDATE rate = VALUES(rate), rate_type = VALUES(rate_type);

-- ------------------------------------------------------------
-- 9. 社保个税政策
-- ------------------------------------------------------------
INSERT INTO hr_social_policy (id, policy_name, effective_date, social_rate, fund_rate, tax_threshold, tax_rate1, tax_rate2, tax_rate3, tax_rate4, tax_rate5, tax_rate6, tax_rate7, quick_deduction, tax_deduction1, tax_deduction2, tax_deduction3, tax_deduction4, tax_deduction5, tax_deduction6, tax_deduction7, status, remark) VALUES
(99001, '2026年度社保个税政策', '2026-01-01', 10.5, 7.0, 5000, 3, 10, 20, 25, 30, 35, 45, 0, 0, 2520, 16920, 31920, 52920, 85920, 181920, 0, '综合所得个税预扣预缴税率表')
ON DUPLICATE KEY UPDATE policy_name = VALUES(policy_name), social_rate = VALUES(social_rate),
    fund_rate = VALUES(fund_rate), tax_threshold = VALUES(tax_threshold), status = VALUES(status),
    tax_deduction1 = VALUES(tax_deduction1), tax_deduction2 = VALUES(tax_deduction2),
    tax_deduction3 = VALUES(tax_deduction3), tax_deduction4 = VALUES(tax_deduction4),
    tax_deduction5 = VALUES(tax_deduction5), tax_deduction6 = VALUES(tax_deduction6),
    tax_deduction7 = VALUES(tax_deduction7), remark = VALUES(remark);

-- ------------------------------------------------------------
-- 10. 工作流：示例流程定义（采购订单三级审批 + 金额条件分支）
-- ------------------------------------------------------------
INSERT INTO wf_definition (id, definition_key, definition_name, biz_type, description, status, version, remark) VALUES
(99101, 'PA_ORDER_APPROVE', '采购订单审批', 'PA_ORDER', '金额 5 万以下由采购主管审批，5 万及以上增加财务总监审批', 1, 1, '示例流程')
ON DUPLICATE KEY UPDATE definition_name = VALUES(definition_name), biz_type = VALUES(biz_type),
    description = VALUES(description), status = VALUES(status), remark = VALUES(remark);

INSERT INTO wf_node (id, definition_id, node_key, node_name, node_type, sort_no, approver_type, approver_value, multi_approve, condition_expr, timeout_hours, remark) VALUES
(99201, 99101, 'SUBMIT', '提交申请', 1, 1, 2, '', 1, '', 0, '发起节点'),
(99202, 99101, 'PURCHASE_MANAGER', '采购主管审批', 2, 2, 1, '3', 1, '', 24, '采购部主管'),
(99203, 99101, 'FINANCE_DIRECTOR', '财务总监审批', 2, 3, 1, '2', 1, 'amount > 50000', 48, '金额超过 5 万时触发'),
(99204, 99101, 'END', '结束', 4, 4, 2, '', 1, '', 0, '结束节点')
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), node_type = VALUES(node_type),
    sort_no = VALUES(sort_no), approver_type = VALUES(approver_type), approver_value = VALUES(approver_value),
    multi_approve = VALUES(multi_approve), condition_expr = VALUES(condition_expr),
    timeout_hours = VALUES(timeout_hours), remark = VALUES(remark);

-- ------------------------------------------------------------
-- 11. 生产：BOM 示例
-- ------------------------------------------------------------
INSERT INTO pm_bom (id, bom_no, material_id, version, status, base_qty, effective_date, is_default, remark) VALUES
(99301, 'BOM-PANEL-A-V1', 45001, 'V1', 1, 1, '2026-01-01', 1, '控制面板总成 BOM')
ON DUPLICATE KEY UPDATE version = VALUES(version), status = VALUES(status), base_qty = VALUES(base_qty),
    effective_date = VALUES(effective_date), is_default = VALUES(is_default), remark = VALUES(remark);

INSERT INTO pm_bom_entry (id, bom_id, material_id, qty, loss_rate, position_no, substitute_flag, remark) VALUES
(99401, 99301, 46001, 1.0000, 2.0000, 1, 0, '连接线束'),
(99402, 99301, 45002, 1.0000, 1.0000, 2, 0, '驱动电机总成'),
(99403, 99301, 44002, 2.0000, 3.0000, 3, 0, 'ABS工程塑料'),
(99404, 99301, 44001, 0.5000, 5.0000, 4, 0, '冷轧钢板')
ON DUPLICATE KEY UPDATE qty = VALUES(qty), loss_rate = VALUES(loss_rate),
    position_no = VALUES(position_no), substitute_flag = VALUES(substitute_flag), remark = VALUES(remark);

-- ------------------------------------------------------------
-- 12. 生产：工作中心与工艺路线
-- ------------------------------------------------------------
INSERT INTO pm_work_center (id, center_code, center_name, capacity_hours, status, remark) VALUES
(99501, 'WC-ASSY', '装配线', 160, 0, '总装车间'),
(99502, 'WC-TEST', '测试线', 120, 0, '测试车间')
ON DUPLICATE KEY UPDATE center_name = VALUES(center_name), capacity_hours = VALUES(capacity_hours),
    status = VALUES(status), remark = VALUES(remark);

INSERT INTO pm_routing (id, routing_no, material_id, version, status, effective_date, remark) VALUES
(99601, 'RT-PANEL-A-V1', 45001, 'V1', 1, '2026-01-01', '控制面板总成工艺路线')
ON DUPLICATE KEY UPDATE version = VALUES(version), status = VALUES(status),
    effective_date = VALUES(effective_date), remark = VALUES(remark);

INSERT INTO pm_routing_operation (id, routing_id, operation_no, operation_name, work_center_id, standard_hours, standard_qty, labor_rate, overhead_rate, sort_no, remark) VALUES
(99701, 99601, 'OP10', '备料', 99501, 0.5, 1, 45.0000, 20.0000, 1, NULL),
(99702, 99601, 'OP20', '装配', 99501, 2.0, 1, 45.0000, 20.0000, 2, NULL),
(99703, 99601, 'OP30', '测试', 99502, 1.0, 1, 50.0000, 25.0000, 3, NULL)
ON DUPLICATE KEY UPDATE operation_name = VALUES(operation_name), work_center_id = VALUES(work_center_id),
    standard_hours = VALUES(standard_hours), standard_qty = VALUES(standard_qty),
    labor_rate = VALUES(labor_rate), overhead_rate = VALUES(overhead_rate), sort_no = VALUES(sort_no),
    remark = VALUES(remark);

-- ------------------------------------------------------------
-- 13. 生产：自制件 BOM（电机总成）
-- ------------------------------------------------------------
INSERT INTO pm_bom (id, bom_no, material_id, version, status, base_qty, effective_date, is_default, remark) VALUES
(99302, 'BOM-MOTOR-B-V1', 45002, 'V1', 1, 1, '2026-01-01', 1, '驱动电机总成 BOM')
ON DUPLICATE KEY UPDATE version = VALUES(version), status = VALUES(status),
    effective_date = VALUES(effective_date), is_default = VALUES(is_default), remark = VALUES(remark);

INSERT INTO pm_bom_entry (id, bom_id, material_id, qty, loss_rate, position_no, substitute_flag, remark) VALUES
(99405, 99302, 44001, 3.0000, 5.0000, 1, 0, '冷轧钢板'),
(99406, 99302, 44003, 2.0000, 2.0000, 2, 1, '贴片电阻(可替代：RM-ELEC-02)'),
(99407, 99302, 44004, 4.0000, 2.0000, 3, 0, '贴片电容')
ON DUPLICATE KEY UPDATE qty = VALUES(qty), loss_rate = VALUES(loss_rate),
    position_no = VALUES(position_no), substitute_flag = VALUES(substitute_flag), remark = VALUES(remark);








