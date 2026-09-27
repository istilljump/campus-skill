export const ORDER_STATUS = {
  1: { label: '待支付', type: 'info' },
  2: { label: '待接单', type: 'warning' },
  3: { label: '进行中', type: 'primary' },
  4: { label: '已送达', type: 'success' },
  5: { label: '已完成', type: 'success' },
  6: { label: '已取消', type: 'danger' },
  7: { label: '已超时', type: 'info' },
}

export const AUDIT_STATUS = {
  0: { label: '待认证', type: 'info' },
  1: { label: '已认证', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
  3: { label: '审核中', type: 'warning' },
}

export const AUDIT_RECORD_STATUS = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

export const WITHDRAW_STATUS = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '已打款', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

export const CANCEL_BY = {
  1: '用户',
  2: '跑腿员',
  3: '平台',
}

export function fmtTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

export function fmtMoney(value) {
  if (value === null || value === undefined) return '-'
  return `¥${Number(value).toFixed(2)}`
}
