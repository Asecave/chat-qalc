package com.vlad2305m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.vlad2305m.ChatqalcClient;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
    @Shadow protected TextFieldWidget chatField;

    @Unique private int messageHistorySize;
    @Shadow public void sendMessage(String chatText, boolean addToHistory) {}

    @Inject(at = @At("HEAD"), method = "keyPressed(Lnet/minecraft/client/input/KeyInput;)Z", cancellable = true)
    public void keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        Runnable exit = ()->{
            cir.setReturnValue(true);
            messageHistorySize++;
        };
        if (input.getKeycode() == 257) {
            if (input.modifiers() == 2 && ChatqalcClient.executeToChat(chatField)) exit.run();
            else if (input.modifiers() == 3 && ChatqalcClient.executeAndBroadcast(chatField, (s)->sendMessage(s, false))) exit.run();
            else if (input.modifiers() == 1 && ChatqalcClient.executeToInput(chatField)) exit.run();
        } else if (input.getKeycode() == 258) {
            if (input.modifiers() == 2 && ChatqalcClient.substituteWord(chatField)) exit.run();
            else if (input.modifiers() == 1 && ChatqalcClient.getCompletions(chatField)) exit.run();
        } else if (input.getKeycode() == 335 && ChatqalcClient.executeToChat(chatField)) exit.run();
    }
}
