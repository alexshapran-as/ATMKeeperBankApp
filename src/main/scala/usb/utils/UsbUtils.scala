package usb.utils

import javax.usb.UsbDevice

import scala.util.matching.Regex

case class UsbUtils() {
  val usb4java: Usb4JavaHigh = new Usb4JavaHigh()
  val devicePattern: Regex = """Device \d*""".r
  def validateBBBUsbConnection(): Boolean = {
//    val beagleBoneUsbDevice: UsbDevice = usb4java.findDevice(0x1d6b.toShort, 0x0104.toShort)
//    if (beagleBoneUsbDevice != null) {
//      devicePattern findFirstIn beagleBoneUsbDevice.toString match {
//        case Some("Device 006") =>
//          println("Beagle Bone is connected")
//          true
//        case None => false
//      }
//    } else {
//      false
//    }
    true // ДЛЯ ТЕСТОВ
  }
}
