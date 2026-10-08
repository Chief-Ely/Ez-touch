sed -i '/val imgCenterLogo = menuView.findViewById<ImageView>(R.id.imgCenterLogo)/a \        imgCenterLogo?.colorFilter = null' app/src/main/java/com/assistivetouch/custom/FloatingService.kt
