import javax.usb.event.{UsbPipeDataEvent, UsbPipeErrorEvent, UsbPipeListener}
import javax.usb.{UsbConfiguration, UsbDevice, UsbDeviceDescriptor, UsbEndpoint, UsbException, UsbHostManager,
  UsbHub, UsbInterface, UsbInterfacePolicy, UsbPipe, UsbServices}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success}

object Usb4Scala {
  def getUsbRootHoob: UsbDevice = {
    scala.util.Try {
      val services: UsbServices = UsbHostManager.getUsbServices
      services.getRootUsbHub
    } match {
      case Success(value) => value
      case Failure(exception) => sys.error(exception.toString)
    }
  }

  def getDevice(vendorId: Short, productId: Short): UsbDevice =
    findDevice(getUsbRootHoob.asInstanceOf[UsbHub], vendorId, productId)

  def findDevice(hub: UsbHub, vendorId: Short, productId: Short): UsbDevice = {
    val devices = hub.getAttachedUsbDevices.toList
    println(devices)
    var resDevice: UsbDevice = null
    devices foreach { case device: UsbDevice =>
      val desc: UsbDeviceDescriptor = device.getUsbDeviceDescriptor
      if (desc.idVendor() == vendorId && desc.idProduct() == productId)
        resDevice = device
      if (device.isUsbHub) {
        resDevice = findDevice(device.asInstanceOf[UsbHub], vendorId, productId)
        if (device != null)
          resDevice = device
      }
    }
    resDevice
  }

    def readMessage(iface: UsbInterface, endPoint: Int): Unit = {
      var pipe: UsbPipe = null
      scala.util.Try {
        iface.claim(new UsbInterfacePolicy() {
          @Override
          def forceClaim(usbInterface: UsbInterface): Boolean = {
            true
          }
        });

        val endpoint: UsbEndpoint = iface.getUsbEndpoints.get(endPoint).asInstanceOf[UsbEndpoint] // there can be more 1,2,3..
        pipe = endpoint.getUsbPipe
        pipe.open()

        var data: Array[Byte] = Array.emptyByteArray
        val received: Int = pipe.syncSubmit(data)
        println(received + " bytes received");

        pipe.close();

      } match {
        case Success(_) => iface.release()
        case Failure(exception) => exception.printStackTrace()
      }
    }

    def readMessageAsynch(iface: UsbInterface, endPoint: Int): Unit = {
      var pipe: UsbPipe = null
      scala.util.Try {
        iface.claim(new UsbInterfacePolicy() {
          @Override
          def forceClaim(usbInterface: UsbInterface): Boolean = {
            true
          }
        })

        val endpoint: UsbEndpoint = iface.getUsbEndpoints.get(endPoint).asInstanceOf[UsbEndpoint] // there can be more 1,2,3..
        pipe = endpoint.getUsbPipe

        pipe.open()

        pipe.addUsbPipeListener(new UsbPipeListener()
        {
          @Override
          def errorEventOccurred(event: UsbPipeErrorEvent): Unit = {
            val error: UsbException = event.getUsbException
            error.printStackTrace()
          }

          @Override
          def dataEventOccurred(event: UsbPipeDataEvent): Unit = {
            var data: Array[Byte] = event.getData
            println(data + " bytes received");
          }
        });

        //			pipe.close();

      } match {
        case Success(_) => iface.release()
        case Failure(exception) => exception.printStackTrace()
      }
    }

    def getDeviceInterface(device: UsbDevice, index: Int): UsbInterface = {
      val configuration: UsbConfiguration = device.getActiveUsbConfiguration
      val iface: UsbInterface = configuration.getUsbInterfaces.get(index).asInstanceOf[UsbInterface] // there can be more 1,2,3..
      iface
    }


    def sendBulkMessage(iface: UsbInterface, message: String, index: Int): Unit = {
      var pipe: UsbPipe = null

      scala.util.Try {
        iface.claim(new UsbInterfacePolicy() {
          @Override
          def forceClaim(usbInterface: UsbInterface): Boolean = {
            true
          }
        })

        val endpoint: UsbEndpoint = iface.getUsbEndpoints.get(index).asInstanceOf[UsbEndpoint]
        pipe = endpoint.getUsbPipe
        pipe.open()

        var initEP: Array[Byte] = Array(0x1b, '@').map(_.toByte)
        var cutP: Array[Byte] =  Array(0x1d, 'V', 1).map(_.toByte)

        val str: String = "nnnnnnnnn"

        pipe.syncSubmit(initEP);
        val sent: Int = pipe.syncSubmit(message.getBytes());
        pipe.syncSubmit(str.getBytes());
        pipe.syncSubmit(cutP);

        println(sent + " bytes sent");
        pipe.close();

      } match {
        case Success(_) => iface.release()
        case Failure(exception) => exception.printStackTrace()
      }
    }
}

//public class Usb4JavaHigh {
//
//

//

//
//

//}
