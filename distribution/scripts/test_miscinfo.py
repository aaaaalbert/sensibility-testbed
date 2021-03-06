import androidlog, miscinfo, sys, time
l = androidlog.log

l("Do the info....")

l("Wifi enabled:")
l(repr(miscinfo.is_wifi_enabled()))
l("Wifi State info:")
l(repr(miscinfo.get_wifi_state()))
l("Wifi Connection info:")
l(repr(miscinfo.get_wifi_connection_info()))
l("Wifi Scan info:")
l(repr(miscinfo.get_wifi_scan_info()))
l("Bluetooth info:")
l(repr(miscinfo.get_bluetooth_info()))
l("Bluetooth scan info:")
l(repr(miscinfo.get_bluetooth_scan_info()))
l("Network Info")
l(repr(miscinfo.get_network_info()))
l("Cellular Provider Info")
l(repr(miscinfo.get_cellular_provider_info()))
l("Cell Info")
l(repr(miscinfo.get_cell_info()))
l("Sim Info")
l(repr(miscinfo.get_sim_info()))
l("Phone Info")
l(repr(miscinfo.get_phone_info()))
l("Mode Settings")
l(repr(miscinfo.get_mode_settings()))
l("Display Info")
l(repr(miscinfo.get_display_info()))
l("Volume Info")
l(repr(miscinfo.get_volume_info()))
l("Battery Info:")
l(repr(miscinfo.get_battery_info()))