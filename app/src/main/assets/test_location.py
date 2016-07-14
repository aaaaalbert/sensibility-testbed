import androidlog, location, sys, time
l = androidlog.log
l('Lets do the location')
while True:
    time.sleep(2)
    loc = location.get_location()
    l("Location: " + str(loc))
    time.sleep(2)
    lkloc = location.get_lastknown_location()
    l("Last Known Location: " + str(lkloc))
    time.sleep(2)
    if not loc:
        continue

    fused = loc.get("fused")
    if not fused:
        continue

    lon = fused.get("longitude")
    lat = fused.get("latitude")
    l("Get address for - lon: " + str(lon) + ", lat: " + str(lat))
    address = location.get_geolocation(lat, lon, 2)
    l(repr(address))

l('KTHXBI')