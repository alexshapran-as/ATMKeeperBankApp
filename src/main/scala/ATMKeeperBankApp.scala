import services.ApiService._
import Usb4Scala._
import javax.usb.UsbDevice

object ATMKeeperBankApp {
  def main(args: Array[String]) {
    val beagleBone: UsbDevice = Usb4Scala.getDevice(0x1d6b.toShort, 0x0104.toShort)
    println(beagleBone)
    Usb4Scala.sendBulkMessage(Usb4Scala.getDeviceInterface(beagleBone, 0), "Hello", 0)
    Routing().routingStart()
  }
}
