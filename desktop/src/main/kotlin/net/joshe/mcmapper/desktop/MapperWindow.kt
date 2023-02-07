package net.joshe.mcmapper.desktop

import java.awt.*
import java.awt.event.ItemEvent.SELECTED
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.JFrame
import javax.swing.ScrollPaneConstants.*
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.joshe.mcmapper.common.ClientData

class MapperWindow(baseUrl: String, opts: MapDisplayOptions) : JFrame("mcmapper") {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val clientData = ClientData(baseUrl)
    private val worldsMenu = JMenu("Worlds")
    private val mapsMenu = JMenu("Maps")
    private val image = MapImage(clientData, opts)

    init {
        val modKey = Toolkit.getDefaultToolkit().menuShortcutKeyMask
        val mainMenu = JMenu("File")
        addOptionsMenu(opts, mainMenu)
        mainMenu.addSeparator()

        val reloadItem = JMenuItem("Reload")
        reloadItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_R, modKey)
        reloadItem.addActionListener {
            scope.launch { clientData.reloadWorldCache(clientData.currentWorld.value?.worldId) }
        }
        mainMenu.add(reloadItem)

        val quitItem = JMenuItem("Quit")
        quitItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_Q, modKey)
        quitItem.addActionListener { exitProcess(0) }
        mainMenu.add(quitItem)

        scope.launch { clientData.worldInfo.collect { root ->
            worldsMenu.removeAll()
            root.entries.sortedBy{it.value.label}.forEach { (key, world) ->
                val item = JMenuItem(world.label)
                item.addActionListener { scope.launch { clientData.selectWorld(key) } }
                worldsMenu.add(item)
            }
            mapsMenu.removeAll()
        }}

        scope.launch { clientData.currentWorld.collect { world ->
            mapsMenu.removeAll()
            world?.maps?.entries?.sortedBy{it.value.label}?.forEach { (key, map) ->
                val item = JMenuItem(map.label)
                item.addActionListener { scope.launch { clientData.selectMap(key) } }
                mapsMenu.add(item)
            }
        }}

        val scroll = JScrollPane(image, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED)

        jMenuBar = JMenuBar()
        jMenuBar.add(mainMenu)
        jMenuBar.add(worldsMenu)
        jMenuBar.add(mapsMenu)
        add(scroll)
        defaultCloseOperation = EXIT_ON_CLOSE
    }

    fun load() {
        scope.launch { clientData.loadRootData() }
    }

    private fun addOptionsMenu(opts: MapDisplayOptions, menu: JMenu) {
        // XXX https://docs.oracle.com/javase/8/docs/api/javax/swing/UIManager.html
        menu.add(JCheckBoxMenuItem("Dark mode", opts.darkMode.value == true).also { item ->
            item.addItemListener { ev -> opts.darkMode.value = (ev.stateChange == SELECTED) }})

        menu.add(JCheckBoxMenuItem("Map IDs", opts.tileIds.value).also { item ->
            item.addItemListener { ev -> opts.tileIds.value = (ev.stateChange == SELECTED) }})

        menu.add(JCheckBoxMenuItem("Pointers", opts.pointers.value).also { item ->
            item.addItemListener { ev -> opts.pointers.value = (ev.stateChange == SELECTED) }})

        menu.add(JCheckBoxMenuItem("Banners", opts.banners.value).also { item ->
            item.addItemListener { ev -> opts.banners.value = (ev.stateChange == SELECTED) }})

        menu.add(JCheckBoxMenuItem("Routes", opts.routes.value).also { item ->
            item.addItemListener { ev -> opts.routes.value = (ev.stateChange == SELECTED) }})
    }
}
