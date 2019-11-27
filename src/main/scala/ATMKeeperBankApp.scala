import services.ApiService._
import usb.utils.UsbUtils

object ATMKeeperBankApp {
  def main(args: Array[String]) {
//    UsbUtils().validateBBBUsbConnection()
    Routing().routingStart()
  }
}
