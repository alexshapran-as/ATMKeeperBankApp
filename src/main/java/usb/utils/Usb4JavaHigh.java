package usb.utils;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import java.util.List;

public class Usb4JavaHigh {

    public UsbDevice getUsbRootHoob() {

        try {
            final UsbServices services = UsbHostManager.getUsbServices();
            return services.getRootUsbHub();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (UsbException e) {
            e.printStackTrace();
        }

        return null;
    }


    public UsbDevice findDevice(short vendorId, short productId) {

        return findDevice((UsbHub) getUsbRootHoob(), vendorId, productId);
    }


    public UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
        System.out.println("Usb Devices: " + hub.getAttachedUsbDevices());
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId)
                return device;
            if (device.isUsbHub()) {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null)
                    return device;
            }
        }
        return null;
    }



    public void readMessage(UsbInterface iface,
                            int endPoint){

        UsbPipe pipe = null;

        try {
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    return true;
                }
            });

            UsbEndpoint endpoint = (UsbEndpoint) iface.getUsbEndpoints().get(endPoint); // there can be more 1,2,3..
            pipe = endpoint.getUsbPipe();
            pipe.open();

            byte[] data = new byte[8];
            int received = pipe.syncSubmit(data);
            System.out.println(received + " bytes received");

            pipe.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                iface.release();
            } catch (UsbClaimException e) {
                e.printStackTrace();
            } catch (UsbNotActiveException e) {
                e.printStackTrace();
            } catch (UsbDisconnectedException e) {
                e.printStackTrace();
            } catch (UsbException e) {
                e.printStackTrace();
            }
        }

    }

    public void readMessageAsynch(UsbInterface iface,
                                  int endPoint){

        UsbPipe pipe = null;

        try {
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    return true;
                }
            });

            UsbEndpoint endpoint = (UsbEndpoint) iface.getUsbEndpoints().get(endPoint); // there can be more 1,2,3..
            pipe = endpoint.getUsbPipe();

            pipe.open();

            pipe.addUsbPipeListener(new UsbPipeListener()
            {
                @Override
                public void errorEventOccurred(UsbPipeErrorEvent event)
                {
                    UsbException error = event.getUsbException();
                    error.printStackTrace();
                }

                @Override
                public void dataEventOccurred(UsbPipeDataEvent event)
                {
                    byte[] data = event.getData();

                    System.out.println(data + " bytes received");
                }
            });

//			pipe.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                iface.release();
            } catch (UsbClaimException e) {
                e.printStackTrace();
            } catch (UsbNotActiveException e) {
                e.printStackTrace();
            } catch (UsbDisconnectedException e) {
                e.printStackTrace();
            } catch (UsbException e) {
                e.printStackTrace();
            }
        }

    }


    public UsbInterface getDeviceInterface(UsbDevice device, int index) {

        UsbConfiguration configuration = device.getActiveUsbConfiguration();
        UsbInterface iface = (UsbInterface) configuration.getUsbInterfaces().get(index); // there can be more 1,2,3..

        return iface;
    }


    public void sendBulkMessage(UsbInterface iface, String message,
                                int index) {

        UsbPipe pipe = null;

        try {
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    return true;
                }
            });

            UsbEndpoint endpoint = (UsbEndpoint) iface.getUsbEndpoints().get(index);
            pipe = endpoint.getUsbPipe();
            pipe.open();

            byte[] initEP = new byte[] { 0x1b, '@' };
            byte[] cutP = new byte[] { 0x1d, 'V', 1 };

            String str = "nnnnnnnnn";

            pipe.syncSubmit(initEP);
            int sent = pipe.syncSubmit(message.getBytes());
            pipe.syncSubmit(str.getBytes());
            pipe.syncSubmit(cutP);

            System.out.println(sent + " bytes sent");
            pipe.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                iface.release();
            } catch (UsbClaimException e) {
                e.printStackTrace();
            } catch (UsbNotActiveException e) {
                e.printStackTrace();
            } catch (UsbDisconnectedException e) {
                e.printStackTrace();
            } catch (UsbException e) {
                e.printStackTrace();
            }
        }
    }
}