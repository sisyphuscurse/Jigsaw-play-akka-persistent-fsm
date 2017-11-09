package com.yiguan.order.service.order

import akka.persistence.fsm.PersistentFSM.FSMState

sealed trait OrderState extends FSMState

case object Created extends OrderState {
  override def identifier: String = "Created"
}
case object Paid extends OrderState {
  override def identifier: String = "Paid"
}
case object Cancelled extends OrderState {
  override def identifier: String = "Cancelled"
}
case object InDelivery extends OrderState {
  override def identifier: String = "InDelivery"
}
case object Received extends OrderState {
  override def identifier: String = "Received"
}
case object Confirmed extends OrderState {
  override def identifier: String = "Confirmed"
}