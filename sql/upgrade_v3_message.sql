-- v3 升级：消息中心
-- 双端共用收件箱：recipient_id 统一存 userId（跑腿员账号即用户账号）
use campus_runner;

create table if not exists message (
  id bigint auto_increment comment '主键' primary key,
  recipient_id bigint not null comment '收件人用户ID',
  title varchar(64) not null comment '消息标题',
  content varchar(255) null comment '消息内容',
  order_id bigint null comment '关联订单ID',
  is_read tinyint not null default 0 comment '是否已读 0未读 1已读',
  create_time datetime not null comment '创建时间',
  key idx_recipient (recipient_id, is_read, id)
) comment '站内消息';
